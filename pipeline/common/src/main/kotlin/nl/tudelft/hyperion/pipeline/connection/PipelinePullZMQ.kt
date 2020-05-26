package nl.tudelft.hyperion.pipeline.connection

import nl.tudelft.hyperion.pipeline.PeerConnectionInformation
import org.zeromq.SocketType
import org.zeromq.ZContext

/**
 * ZMQ implementation of message pull from previous pipeline plugin.
 */
class PipelinePullZMQ {
    private val ctx = ZContext()
    private val socket = ctx.createSocket(SocketType.PULL)

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
     * Receives string from ZMQ socket blocking.
     */
    fun pull(): String {
        return socket.recvStr()
    }

    /**
     * Closes the ZMQ socket and destroys the context blocking.
     */
    fun closeConnection() {
        socket.close()
        ctx.destroy()
    }
}
