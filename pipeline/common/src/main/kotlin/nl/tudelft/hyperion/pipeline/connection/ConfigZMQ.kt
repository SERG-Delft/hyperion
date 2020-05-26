package nl.tudelft.hyperion.pipeline.connection

import org.zeromq.SocketType
import org.zeromq.ZContext

/**
 * Requests the config from [PluginManager] via ZMQ connection
 */
class ConfigZMQ(pluginManager: String) {

    private val logger = mu.KotlinLogging.logger {}
    private val pluginManagerHost = "tcp://$pluginManager"

    fun requestConfig(id: String, type: String): String {
        val context = ZContext()
        val socket = context.createSocket(SocketType.REQ)
        val req = """{"id":"$id","type":"$type"}"""

        logger.debug { "Connecting to $pluginManagerHost" }
        socket.connect(pluginManagerHost)

        logger.debug { "Sending message $req" }
        socket.send(req)

        val rep = socket.recvStr()
        logger.debug { "Received $rep" }

        socket.close()
        context.destroy()

        return rep
    }
}
