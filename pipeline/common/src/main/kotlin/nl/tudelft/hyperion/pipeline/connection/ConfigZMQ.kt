package nl.tudelft.hyperion.pipeline.connection

import nl.tudelft.hyperion.pipeline.PeerConnectionInformation
import org.zeromq.SocketType

/**
 * Requests the config from [PluginManager] via ZMQ connection
 */
class ConfigZMQ(pluginManager: String) {

    private val logger = mu.KotlinLogging.logger {}
    private val pluginManagerHost = "tcp://$pluginManager"

    fun requestConfig(id: String, type: ConfigType): String {
        logger.debug { "Connecting to $pluginManagerHost" }
        val conn = SetupZMQConnection(SocketType.REQ)
        conn.setupConnection(PeerConnectionInformation(pluginManagerHost, false))

        val req = """{"id":"$id","type":"${type.toString().toLowerCase()}"}"""
        logger.debug { "Sending message $req" }
        conn.socket.send(req)

        val rep = conn.socket.recvStr()
        logger.debug { "Received $rep" }

        conn.closeConnection()

        return rep
    }
}
