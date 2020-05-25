package nl.tudelft.hyperion.pipeline.connection

/**
 * Interface used by :AbstractPipelinePlugin: to push data to the next plugin.
 * @param T: type of configuration used by setupConnection()
 */
interface PipelinePush<T> {
    /**
     * Setup the connection with the successive plugin using config of type T.
     * setupConnection is called once in the :AbstractPipelinePlugin: before
     * calls to push() are made.
     */
    fun setupConnection(config: T)

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
