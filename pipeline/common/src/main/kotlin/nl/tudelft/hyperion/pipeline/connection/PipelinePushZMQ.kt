package nl.tudelft.hyperion.pipeline.connection

import org.zeromq.SocketType

/**
 * ZMQ implementation of message push to successive pipeline plugin.
 */
class PipelinePushZMQ : SetupZMQConnection(SocketType.PUSH) {
    private val logger = mu.KotlinLogging.logger {}

    /**
     * Pushes given string on ZMQ socket blocking.
     * Does not throw.
     */
    @Suppress("TooGenericExceptionCaught")
    fun push(msg: String) {
        try {
            socket.send(msg, zmq.ZMQ.ZMQ_DONTWAIT)
        } catch (ex: Exception) {
            logger.error(ex) { "Error pushing message to ZMQ pipeline" }
        }
    }
}
