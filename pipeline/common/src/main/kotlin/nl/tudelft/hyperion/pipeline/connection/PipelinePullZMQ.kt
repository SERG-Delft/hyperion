package nl.tudelft.hyperion.pipeline.connection

import org.zeromq.SocketType

/**
 * ZMQ implementation of message pull from previous pipeline plugin.
 */
class PipelinePullZMQ : SetupZMQConnection(SocketType.PULL) {
    private val logger = mu.KotlinLogging.logger {}

    /**
     * Receives string from ZMQ socket blocking.
     * When receive fails, "Invalid Message" will be returned.
     * This method does not throw.
     */
    @Suppress("TooGenericExceptionCaught")
    fun pull(): String {
        return try {
            socket.recvStr()
        } catch (ex: Exception) {
            logger.error(ex) { "Error pulling new messages from ZMQ pipeline" }
            "Invalid Message"
        }
    }
}
