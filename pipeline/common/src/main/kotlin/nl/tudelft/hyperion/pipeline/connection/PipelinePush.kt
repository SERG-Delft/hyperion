package nl.tudelft.hyperion.pipeline.connection

import nl.tudelft.hyperion.pipeline.PeerConnectionInformation

/**
 * Interface used by :AbstractPipelinePlugin: to push data to the next plugin.
 */
interface PipelinePush {
    /**
     * Setup the connection to the previous plugin.
     * setupConnection is called once in the :AbstractPipelinePlugin: before
     * calls to push() are made.
     */
    fun setupConnection(config: PeerConnectionInformation)

    /**
     * Pushes the given string over your connection.
     */
    fun push(msg: String)

    /**
     * Do any tearDown your connection requires.
     * closeConnection is called once in the :AbstractPipelinePlugin: after
     * setupConnection is called and possibly multiple push() invocations.
     */
    fun closeConnection()
}
