package nl.tudelft.hyperion.aggregator.intake

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.aggregator.Configuration
import nl.tudelft.hyperion.aggregator.ZMQConfiguration
import nl.tudelft.hyperion.aggregator.api.LogEntry
import nl.tudelft.hyperion.aggregator.workers.AggregationManager
import org.joda.time.DateTime
import org.joda.time.Seconds
import org.zeromq.SocketType
import org.zeromq.ZContext
import java.util.concurrent.Executors

/**
 * Central class that manages intake from the ZeroMQ command channel. Will
 * receive messages from the pipeline, forwarding them to the aggregation
 * manager for eventual aggregation.
 */
class ZMQIntake(
    val pluginConfiguration: Configuration,
    val aggregationManager: AggregationManager,
    val configuration: ZMQConfiguration = pluginConfiguration.pipeline
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

            logger.debug { "Requesting connection information from ${configuration.managerHost}" }

            ZContext().use {
                val socket = it.createSocket(SocketType.REQ)
                socket.connect("tcp://${configuration.managerHost}")

                socket.send("""{"id":"${configuration.id}","type":"pull"}""")
                subInformation = PeerConnectionInformation.parse(socket.recvStr())

                socket.close()

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
    fun listen(): Job {
        val subInfo = subInformation ?: throw ZMQIntakeInitializationException("Must call ZMQIntake.setup first")
        logger.info { "Will listen to log entries sent in '$subInfo'." }

        return receiverScope.launch {
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
    }

    /**
     * Called by redis when we receive a new message. We will ignore the channel,
     * and instead assume that any message on a channel that we've subscribed to
     * contains valid log entries.
     */
    @Suppress("TooGenericExceptionCaught", "ReturnCount")
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

        if (pluginConfiguration.verifyTimestamp) {
            if (entry.timestamp == null) {
                logger.warn {
                    "Received a log entry with a missing timestamp. Cannot verify that the entry fits " +
                    "within the current time period, and will therefore ignore it. You can disable this " +
                    "behavior by setting the `verify-timestamp` property to false in the config."
                }

                return
            }

            val secondsSinceLogEntry = Seconds.secondsBetween(entry.timestamp, DateTime.now()).seconds
            if (secondsSinceLogEntry > pluginConfiguration.granularity) {
                logger.warn {
                    "Received a log entry that happened $secondsSinceLogEntry seconds ago, while the " +
                    "granularity of the aggregator is set to ${pluginConfiguration.granularity} seconds. " +
                    "Will ignore this log entry. You can fix this warning by either increasing your " +
                    "granularity, decreasing the time between log and it arriving at the aggregator, " +
                    "or by setting the `verify-timestamp` to false in the configuration."
                }

                return
            }
        }

        aggregationManager.aggregate(entry)
    }

    /**
     * Helper function for when you already know which connection information
     * you will use to connect. Will throw if the connection information was already
     * set.
     */
    fun setConnectionInformation(info: PeerConnectionInformation) {
        if (subInformation != null) {
            throw ZMQIntakeInitializationException("ZMQIntake is already setup")
        }

        subInformation = info
    }
}

/**
 * Represents the information needed for this aggregator to connect to
 * all the incoming data sources. Sent by the plugin manager.
 */
data class PeerConnectionInformation(
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
