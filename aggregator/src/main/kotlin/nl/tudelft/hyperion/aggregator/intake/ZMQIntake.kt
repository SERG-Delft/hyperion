package nl.tudelft.hyperion.aggregator.intake

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.aggregator.ZMQConfiguration
import nl.tudelft.hyperion.aggregator.api.LogEntry
import nl.tudelft.hyperion.aggregator.workers.AggregationManager
import org.zeromq.SocketType
import org.zeromq.ZContext
import java.util.concurrent.Executors

/**
 * Central class that manages intake from the ZeroMQ command channel. Will
 * receive messages from the pipeline, forwarding them to the aggregation
 * manager for eventual aggregation.
 */
class ZMQIntake(
    val configuration: ZMQConfiguration,
    val aggregationManager: AggregationManager
) {
    private val logger = mu.KotlinLogging.logger {}
    private var subInformation: PeerConnectionInformation? = null

    private val processThreadPool = CoroutineScope(
        Executors
            .newFixedThreadPool(4)
            .asCoroutineDispatcher()
    )
    private val receiverScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    /**
     * Connects to the plugin manager and requests what channels to receive
     * aggregated information on. Will throw if this process fails. Note that
     * this is a blocking call.
     */
    @Suppress("TooGenericExceptionCaught")
    fun setup() {
        try {
            if (subInformation != null) {
                throw ZMQIntakeInitializationException("ZMQIntake is already setup")
            }

            logger.debug { "Requesting connection information from ${configuration.pluginManager}" }

            ZContext().use {
                val socket = it.createSocket(SocketType.REQ)
                socket.connect("tcp://${configuration.pluginManager}")

                socket.send("""{"id":"${configuration.id}","type":"in"}""")
                subInformation = PeerConnectionInformation.parse(socket.recvStr())

                logger.debug { "subInformation: $subInformation" }
            }

            logger.debug { "Successfully retrieved connection information" }
        } catch (ex: Exception) {
            throw ZMQIntakeInitializationException("Failed to setup ZMQ intake", ex)
        }
    }

    /**
     * Starts a new listener worker that will listen on the queried ZMQ port
     * for any new messages that need to be aggregated. Returns a job that,
     * when cancelled, will clean up after itself.
     */
    fun listen() = receiverScope.launch {
        val subInfo = subInformation ?: throw ZMQIntakeInitializationException("Must call ZMQIntake.setup first")
        logger.info { "Will listen to log entries sent in '$subInfo'." }

        val ctx = ZContext()
        val sock = ctx.createSocket(SocketType.PULL)

        if (subInfo.isBind) {
            sock.bind(subInfo.host)
        } else {
            sock.connect(subInfo.host)
        }

        while (isActive) {
            val msg = sock.recvStr()

            // Handle this message in the threadpool.
            processThreadPool.launch {
                handleMessage(msg)
            }
        }

        sock.close()
        ctx.destroy()
    }

    /**
     * Called by redis when we receive a new message. We will ignore the channel,
     * and instead assume that any message on a channel that we've subscribed to
     * contains valid log entries.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun handleMessage(message: String) {
        logger.debug { "Received message from ZMQ: '$message'" }

        val entry = try {
            LogEntry.parse(message)
        } catch (ex: Exception) {
            logger.warn(ex) {
                "Failed to parse incoming log entry. Are you sure that your pipeline is configured properly?"
            }

            return
        }

        aggregationManager.aggregate(entry)
    }
}

/**
 * Represents the information needed for this aggregator to connect to
 * all the incoming data sources. Sent by the plugin manager.
 */
private data class PeerConnectionInformation(
    val host: String,
    val isBind: Boolean
) {
    companion object {
        /**
         * Converts the specified JSON string into an instance. Throws
         * if the specified string is not valid.
         */
        fun parse(content: String): PeerConnectionInformation {
            val mapper = ObjectMapper(JsonFactory())
            mapper.registerModule(KotlinModule())

            return mapper.readValue(content)
        }
    }
}

/**
 * Represents that an error was encountered during initialization of the ZMQ intake service.
 */
class ZMQIntakeInitializationException(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause)
