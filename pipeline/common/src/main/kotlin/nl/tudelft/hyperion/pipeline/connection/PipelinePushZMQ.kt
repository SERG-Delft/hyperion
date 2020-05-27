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
    private val logger = mu.KotlinLogging.logger {}

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
     * Pushes given string on ZMQ socket blocking.
     * Does not throw.
     */
    @Suppress("TooGenericExceptionCaught")
    fun push(msg: String) {
        try {
            socket.send(msg, zmq.ZMQ.ZMQ_DONTWAIT)
        } catch (ex: Exception) {
            logger.error { ex }
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
