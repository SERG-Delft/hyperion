package nl.tudelft.hyperion.datasource.common

/**
 * Acts as an adapter for any data source to the push-based data pipeline.
 */
interface DataSourcePlugin {

    /**
     * Starts sending logs to the message queue.
     */
    fun start()

    /**
     * Stops sending logs to the message queue, but remains active.
     */
    fun stop()

    /**
     * Cleans up remaining resources or IO and exits.
     *
     * [start] and [stop] will not be callable unless a new instance is created
     */
    fun cleanup()
}
