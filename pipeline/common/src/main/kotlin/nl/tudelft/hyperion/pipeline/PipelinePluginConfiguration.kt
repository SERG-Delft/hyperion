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
     * The connection details for connecting to redis.
     */
    val redis: PipelineRedisConfiguration
)

/**
 * Represents the redis settings needed for the plugin to connect to the
 * main redis pub/sub server.
 */
data class PipelineRedisConfiguration(
    /**
     * The hostname of the redis instance.
     */
    val host: String,
    /**
     * The port of the redis instance. Defaults to 6379
     */
    val port: Int = 6379
)
