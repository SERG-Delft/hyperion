package nl.tudelft.hyperion.pipeline.connection

/**
 * Interface used by :AbstractPipelinePlugin: to pull data from the previous plugin.
 * @param T: type of configuration used by setupConnection()
 */
interface PipelinePull<T> {
    /**
     * Setup the connection with the previous plugin using config of type T.
     * setupConnection is called once in the :AbstractPipelinePlugin: before
     * calls to pull() are made.
     */
    fun setupConnection(config: T)

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
