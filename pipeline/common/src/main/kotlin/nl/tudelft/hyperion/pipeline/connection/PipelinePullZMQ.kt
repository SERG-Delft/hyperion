package nl.tudelft.hyperion.pipeline.connection

import nl.tudelft.hyperion.pipeline.PeerConnectionInformation
import org.zeromq.SocketType
import org.zeromq.ZContext

/**
 * ZMQ implementation of :PipelinePush:
 */
class PipelinePullZMQ : PipelinePull<PeerConnectionInformation> {
    private val ctx = ZContext()
    private val socket = ctx.createSocket(SocketType.PULL)

    override fun setupConnection(config: PeerConnectionInformation) {
        when (config.isBind) {
            true -> socket.bind(config.host)
            false -> socket.connect(config.host)
        }
    }

    override fun pull(): String {
        return socket.recvStr()
    }

    override fun closeConnection() {
        socket.close()
        ctx.destroy()
    }
}
