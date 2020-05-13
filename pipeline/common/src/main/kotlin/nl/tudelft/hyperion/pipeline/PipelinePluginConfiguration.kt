package nl.tudelft.hyperion.pipeline

/**
 * Represents a configuration for a pipeline plugin that is loaded from
 * some file or other representation. Plugins can subclass this configuration
 * to add their own configuration, or build it at runtime. As an added benefit,
 * subclassing this method will expose an easy method to construct the runtime
 * object from a YAML configuration string.
 */
data class PipelinePluginConfiguration(
    /**
     * A unique ID that represents this plugin within the pipeline. May
     * not conflict with other plugins and must match the one set in the
     * plugin manager.
     */
    val id: String,

    /**
     * The host and port that contains the plugin manager this plugin needs
     * to connect to.
     */
    val pluginManager: String
)
