package nl.tudelft.hyperion.pipeline.loadbalancer

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.tudelft.hyperion.pipeline.readJSONContent
import org.zeromq.SocketType
import org.zeromq.ZContext
import java.util.concurrent.Executors

/**
 * Manager that handles the routing of messages to other plugins.
 * The manager uses a REQ/REP socket to route the plugins to the given
 * sink and ventilator ports.
 */
object WorkerManager {
    private val managerScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    private val connected = mutableListOf<String>()
    private val logger = KotlinLogging.logger {}

    /**
     * Starts a listener in a separate thread that responds to workers
     * where the endpoints are for the distributor and sink.
     *
     * @param hostname the hostname of the load balancer
     * @param port the port that the manager should listen on
     * @param sinkPort the port of the sink
     * @param ventilatorPort the port of the distributor
     */
    fun run(hostname: String, port: Int, sinkPort: Int, ventilatorPort: Int) = managerScope.launch {
        val ctx = ZContext()
        val sock = ctx.createSocket(SocketType.REP)

        sock.bind(createAddress(hostname, port))

        while (isActive) {
            var workerInfo: WorkerInfo?

            try {
                workerInfo = readJSONContent(sock.recvStr())

            } catch (e: InvalidFormatException) {
                logger.warn { "Failed to parse client request: ${e.message}" }
                continue
            }

            // does not do failure handling
            val isSuccess = when(workerInfo.type) {
                ConnectionType.PUSH ->
                    sock.send("""{"host":${createAddress(hostname, sinkPort)}, "isBind": "false"}""")

                ConnectionType.PULL ->
                    sock.send("""{"host":${createAddress(hostname, ventilatorPort)}, "isBind": "false"}""")
            }

            // first time worker connects
            if (isSuccess && workerInfo.id !in connected) {
                connected.add(workerInfo.id)
            }
        }

        sock.close()
        ctx.close()
    }
}

enum class ConnectionType {
    @JsonProperty("push")
    PUSH,
    @JsonProperty("pull")
    PULL
}

data class WorkerInfo(
        val id: String,
        val type: ConnectionType
)