package nl.tudelft.hyperion.datasource.common

import kotlinx.coroutines.Job

/**
 * Acts as an adapter for any data source to the push-based data pipeline.
 */
interface DataSourcePlugin {

    /**
     * Starts sending logs to the message queue.
     */
    fun start(): Job

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

/**
 * Exception thrown during initialization of a data source plugin.
 */
class DataPluginInitializationException(
        msg: String,
        cause: Throwable? = null
) : RuntimeException(msg, cause)
