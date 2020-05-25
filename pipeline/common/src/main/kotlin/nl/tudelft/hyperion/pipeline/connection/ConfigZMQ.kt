package nl.tudelft.hyperion.pipeline.connection

import org.zeromq.SocketType
import org.zeromq.ZContext

/**
 * ZMQ implementation of :PluginManagerConnection:
 */
class ConfigZMQ(pluginManager: String) : PluginManagerConnection {

    private val logger = mu.KotlinLogging.logger {}
    private val pluginManagerHost ="tcp://$pluginManager"

    override fun requestConfig(id: String, type: String): String {
        val context = ZContext()
        val socket = context.createSocket(SocketType.REQ)
        val req = """{"id":"$id","type":"$type"}"""

        logger.debug { "Connecting to $pluginManagerHost"}
        socket.connect(pluginManagerHost)

        logger.debug { "Sending message $req" }
        socket.send(req)

        val rep = socket.recvStr()
        logger.debug { "Received $rep"}

        socket.close()
        context.destroy()

        return rep
    }
}
