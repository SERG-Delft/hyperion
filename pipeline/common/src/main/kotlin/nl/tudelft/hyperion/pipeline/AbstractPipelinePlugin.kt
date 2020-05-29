package nl.tudelft.hyperion.pipeline

import kotlinx.coroutines.CoroutineScope
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
    private val logger = mu.KotlinLogging.logger {}
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
    open fun run() = GlobalScope.launch {
        runSuspend(this)
    }

    suspend fun runSuspend(scope: CoroutineScope) {
        if (!hasConnectionInformation) {
            throw PipelinePluginInitializationException("Cannot run plugin without connection information")
        }

        val channel = Channel<String>(capacity = 20_000)
        val sender = runSender(channel)
        val receiver = runReceiver(channel)

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
     * Helper function that will create a new subroutine that is used to send the
     * results of computation to the next stage in the pipeline.
     */
    fun runSender(channel: Channel<String>) = senderScope.launch {
        sink.setupConnection(pubConnectionInformation)

        while (isActive) {
            val msg = channel.receive()
            packetBufferCount.decrementAndGet()
            sink.push(msg)
        }

        sink.closeConnection()
    }

    /**
     * Helper function that will create a new subroutine that is used to receive
     * messages from the previous stage and push it to the process function.
     */
    @Suppress("TooGenericExceptionCaught")
    fun runReceiver(channel: Channel<String>) = receiverScope.launch {
        source.setupConnection(subConnectionInformation)

        while (isActive) {
            val msg = source.pull()

            // Drop this message if our internal buffer is full
            val inQueue = packetBufferCount.incrementAndGet()
            if (inQueue > config.bufferSize) {
                packetBufferCount.decrementAndGet()
                continue
            }

            processThreadPool.launch processLaunch@{
                val result = try {
                    this@AbstractPipelinePlugin.process(msg)
                } catch (ex: Exception) {
                    logger.warn(ex) { "Error processing message: '$msg'" }
                    null
                } ?: run {
                    // We're done here, decrement.
                    packetBufferCount.decrementAndGet()
                    return@processLaunch
                }

                channel.send(result)
            }
        }
    }

    /**
     * Method that performs the plugin transform on the specified input.
     * This method can suspend, thus it can perform asynchronous transformations
     * before returning a result.
     *
     * If this method throws, its result is discarded. Since this will cause
     * data loss, it is recommended to only throw if no other options exist.
     * Returning null will also discard the result, but without the performance
     * cost of throwing/handling an exception.
     */
    abstract suspend fun process(input: String): String?
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
 */
data class PeerConnectionInformation(
    val host: String,
    val isBind: Boolean
)
