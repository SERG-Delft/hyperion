package nl.tudelft.hyperion.pipeline.connection

import nl.tudelft.hyperion.pipeline.PeerConnectionInformation

/**
 * Interface used by :AbstractPipelinePlugin: to pull data from the previous plugin.
 */
interface PipelinePull {
    /**
     * Setup the connection to the previous plugin.
     * setupConnection is called once in the :AbstractPipelinePlugin: before
     * calls to pull() are made.
     */
    fun setupConnection(config: PeerConnectionInformation)

    /**
     * Returns the latest message received from the connection.
     */
    fun pull(): String

    /**
     * Do any tearDown your connection requires.
     * closeConnection is called once in the :AbstractPipelinePlugin: after
     * setupConnection is called and possibly multiple pull() invocations.
     */
    fun closeConnection()
}
