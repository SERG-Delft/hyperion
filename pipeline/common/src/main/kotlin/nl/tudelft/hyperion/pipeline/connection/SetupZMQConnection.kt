package nl.tudelft.hyperion.pipeline.connection

import nl.tudelft.hyperion.pipeline.PeerConnectionInformation
import org.zeromq.SocketType
import org.zeromq.ZContext

/**
 * Provides handlers to setup and teardown ZMQ socket.
 * @param socketType Type of Socket that should be created.
 */
open class SetupZMQConnection(socketType: SocketType) {
    private val ctx = ZContext()
    val socket = ctx.createSocket(socketType)

    /**
     * Setup the ZMQ connection in a blocking fashion.
     */
    fun setupConnection(config: PeerConnectionInformation) {
        if (config.isBind) {
            socket.bind(config.host)
        } else {
            socket.connect(config.host)
        }
    }

    /**
     * Closes the ZMQ socket and destroys the context blocking.
     */
    fun closeConnection() {
        socket.close()
        ctx.destroy()
    }
}
