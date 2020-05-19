package nl.tudelft.hyperion.pipeline.loadbalancer

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.tudelft.hyperion.pipeline.readJSONContent
import org.zeromq.SocketType
import org.zeromq.ZContext
import java.lang.Exception
import java.util.concurrent.Executors

/**
 * Manager that handles the routing of messages to other plugins.
 * The manager uses a REQ/REP socket to route the plugins to the given
 * sink and ventilator ports.
 */
object WorkerManager {
    private val managerScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    private val workerIds = mutableListOf<String>()
    private val logger = KotlinLogging.logger {}

    /**
     * Starts a listener in a separate thread that responds to workers
     * where the endpoints are for the distributor and sink.
     * The Manager assumes that each worker has a unique id.
     *
     * @param hostname the hostname of the load balancer
     * @param port the port that the manager should listen on
     * @param sinkPort the port of the sink
     * @param ventilatorPort the port of the distributor
     */
    @Suppress("TooGenericExceptionCaught")
    fun run(hostname: String, port: Int, sinkPort: Int, ventilatorPort: Int) = managerScope.launch {
        val ctx = ZContext()
        val sock = ctx.createSocket(SocketType.REP)

        logger.debug { "Binding manager to ${createAddress(hostname, port)}" }
        sock.bind(createAddress(hostname, port))

        while (isActive) {
            var workerInfo: WorkerInfo?

            try {
                val content = sock.recvStr()
                workerInfo = readJSONContent(content)
                logger.debug { "Worker request received: $content" }

            } catch (e: Exception) {
                logger.warn { "Failed to parse client request: ${e.message}" }
                continue
            }

            // does not do failure handling
            val isSuccess = when(workerInfo.type) {
                ConnectionType.PUSH ->
                    sock.send("""{"host":"${createAddress(hostname, sinkPort)}", "isBind": "false"}""")

                ConnectionType.PULL ->
                    sock.send("""{"host":"${createAddress(hostname, ventilatorPort)}", "isBind": "false"}""")
            }

            // add worker id to list of connected workers on first request
            if (isSuccess && workerInfo.id !in workerIds) {
                logger.info { "Worker with id=${workerInfo.id} connected" }
                workerIds.add(workerInfo.id)
            }
        }

        sock.close()
        ctx.close()
    }
}

/**
 * Represents the types of connections workers can request.
 */
enum class ConnectionType {
    @JsonProperty("push")
    PUSH,
    @JsonProperty("pull")
    PULL
}

/**
 * Represents a request from a worker plugin.
 *
 * @property id the worker's id
 * @property type the type of the connection
 *  the worker is requesting
 */
data class WorkerInfo(
        val id: String,
        val type: ConnectionType
)