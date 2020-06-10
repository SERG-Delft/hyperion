package nl.tudelft.hyperion.pipeline

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.pipeline.connection.ConfigType
import nl.tudelft.hyperion.pipeline.connection.ConfigZMQ
import nl.tudelft.hyperion.pipeline.connection.PipelinePullZMQ
import nl.tudelft.hyperion.pipeline.connection.PipelinePushZMQ
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * Represents an abstract pipeline plugin that receives some JSON
 * representation of a value and asynchronously transforms it into
 * some other representation. All communication is handled by this
 * class, such that implementors can focus on doing the actual
 * transformation.
 */
abstract class AbstractPipelinePlugin(
    private val config: PipelinePluginConfiguration,
    private val pmConn: ConfigZMQ = ConfigZMQ(config.pluginManager),
    private val sink: PipelinePushZMQ = PipelinePushZMQ(),
    private val source: PipelinePullZMQ = PipelinePullZMQ()
) {
    protected open val logger = mu.KotlinLogging.logger {}
    private val processThreadPool = CoroutineScope(
        Executors
            .newFixedThreadPool(4)
            .asCoroutineDispatcher()
    )
    private val senderScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    private val receiverScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    var hasConnectionInformation = false
    lateinit var subConnectionInformation: PeerConnectionInformation
    lateinit var pubConnectionInformation: PeerConnectionInformation

    protected val canReceive
        get() = hasConnectionInformation && subConnectionInformation.host != null

    protected val canSend
        get() = hasConnectionInformation && pubConnectionInformation.host != null

    protected val isPassthrough
        get() = canReceive && canSend

    private val senderChannel = Channel<String>(20_000)
    private val packetBufferCount = AtomicInteger()

    /**
     * Synchronously attempts to query the connection information from the plugin
     * manager. This will block the calling thread until a response is received
     * from the plugin manager, which may take an arbitrary amount of time depending
     * on whether the service is online.
     */
    fun queryConnectionInformation() {
        if (hasConnectionInformation) {
            throw PipelinePluginInitializationException("Cannot query connection information twice")
        }

        logger.debug { "Requesting connection information from ${config.pluginManager}" }

        subConnectionInformation = readJSONContent(pmConn.requestConfig(config.id, ConfigType.PULL))
        pubConnectionInformation = readJSONContent(pmConn.requestConfig(config.id, ConfigType.PUSH))

        logger.debug { "subConnectionInformation: $subConnectionInformation" }
        logger.debug { "pubConnectionInformation: $pubConnectionInformation" }
        logger.info { "Successfully retrieved connection information" }

        hasConnectionInformation = true
    }

    /**
     * Sets up the ZMQ sockets needed to consume and send messages for this plugin.
     * Returns a job that, when cancelled, will automatically clean up after itself.
     */
    @JvmOverloads
    open fun run(context: CoroutineContext = Dispatchers.Default) = GlobalScope.launch(context) {
        runSuspend(this)
    }

    suspend fun runSuspend(scope: CoroutineScope) {
        if (!hasConnectionInformation) {
            throw PipelinePluginInitializationException("Cannot run plugin without connection information")
        }

        val sender = runSender(senderChannel)
        val receiver = runReceiver()

        // Sleep infinitely
        try {
            while (scope.isActive) {
                delay(Long.MAX_VALUE)
            }
        } finally {
            sender.cancelAndJoin()
            receiver.cancelAndJoin()
        }
    }

    /**
     * Queues the specified message for sending to the next step. If [canSend]
     * is false, will throw an error. Note that this will queue the message in
     * a channel with limited capacity, so sending a large amount of messages
     * at once may discard some.
     *
     * This function exists such that Java plugins can interact with it without
     * needing to know how suspended functions work.
     */
    protected fun sendQueued(message: String) {
        if (!canSend) {
            throw IllegalStateException("Cannot invoke sendQueued on a pipeline plugin that cannot send.")
        }

        senderChannel.offer(message)
    }

    /**
     * Similar to [sendQueued], but instead suspends until the queue is able
     * to accept the specified message.
     */
    protected suspend fun send(message: String) {
        if (!canSend) {
            throw IllegalStateException("Cannot invoke send on a pipeline plugin that cannot send.")
        }

        senderChannel.send(message)
    }

    /**
     * Helper function that will create a new subroutine that is used to send the
     * results of computation to the next stage in the pipeline.
     */
    fun runSender(channel: Channel<String>) = senderScope.launch {
        if (!canSend) {
            return@launch
        }

        sink.setupConnection(pubConnectionInformation)

        while (isActive) {
            val msg = channel.receive()
            sink.push(msg)
        }

        sink.closeConnection()
    }

    /**
     * Helper function that will create a new subroutine that is used to receive
     * messages from the previous stage and push it to the process function.
     */
    fun runReceiver() = receiverScope.launch {
        if (!canReceive) {
            return@launch
        }

        source.setupConnection(subConnectionInformation)

        while (isActive) {
            val msg = source.pull()
            logger.trace { "Received message: '$msg'" }

            // Check for buffer limits if this is passthrough.
            if (isPassthrough) {
                // Drop this message if our internal buffer is full
                val inQueue = packetBufferCount.incrementAndGet()
                if (inQueue > config.bufferSize) {
                    packetBufferCount.decrementAndGet()
                    continue
                }
            }

            // Within the thread pool, handle the message and decrement the counter.
            processThreadPool.launch {
                onMessageReceived(msg)

                if (isPassthrough) {
                    packetBufferCount.decrementAndGet()
                }
            }
        }
    }

    /**
     * Method invoked when a message is received. Note that this method will
     * only be invoked if [canReceive] is true. In other cases (such as when
     * implementing a data source), this method can simply be stubbed out. If
     * you want to perform transformation on the input and then send it out,
     * consider subclassing [TransformingPipelinePlugin] instead.
     *
     * This function is invoked on a thread pool, so multiple messages are able
     * to be handled in parallel.
     */
    abstract suspend fun onMessageReceived(msg: String)
}

/**
 * Exception thrown during initialization of a pipeline plugin.
 */
class PipelinePluginInitializationException(
    msg: String,
    cause: Throwable? = null
) : RuntimeException(msg, cause)

/**
 * Represents the information needed for this plugin to connect to any
 * future elements in the pipeline. Contains the host, port and whether
 * it needs to bind or connect to that specific element.
 *
 * If the host is null, it means that the plugin manager has no
 * adjacent plugin available for that direction (i.e. this is either
 * the first or the last plugin).
 */
data class PeerConnectionInformation(
    val host: String?,
    val isBind: Boolean
)
