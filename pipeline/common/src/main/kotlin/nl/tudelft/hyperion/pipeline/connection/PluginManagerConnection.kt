package nl.tudelft.hyperion.pipeline.connection

/**
 * Realises a connection with the PluginManager
 */
interface PluginManagerConnection {

    /**
     * Returns configuration for the given plugin (id) and type (push/pull)
     */
    fun requestConfig(id: String, type: String): String
}
