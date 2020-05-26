package nl.tudelft.hyperion.pipeline.connection

import nl.tudelft.hyperion.pipeline.PeerConnectionInformation
import org.zeromq.SocketType
import org.zeromq.ZContext

/**
 * ZMQ implementation of message push to successive pipeline plugin.
 */
class PipelinePushZMQ {
    private val ctx = ZContext()
    private val socket = ctx.createSocket(SocketType.PUSH)

    fun setupConnection(config: PeerConnectionInformation) {
        if (config.isBind) {
            socket.bind(config.host)
        } else {
            socket.connect(config.host)
        }
    }

    fun push(msg: String) {
        socket.send(msg, zmq.ZMQ.ZMQ_DONTWAIT)
    }

    fun closeConnection() {
        socket.close()
        ctx.destroy()
    }
}
