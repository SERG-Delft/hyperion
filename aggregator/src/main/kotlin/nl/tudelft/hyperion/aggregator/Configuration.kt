package nl.tudelft.hyperion.aggregator

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.nio.file.Files
import java.nio.file.Path

/**
 * Represents a YAML configuration file for the aggregator.
 * Contains the database URL, web port and granularity of the
 * metrics.
 */
data class Configuration(
    /**
     * The JDBC url to the database.
     */
    @JsonProperty("database-url")
    val databaseUrl: String,
    /**
     * The port on which the web service should listen.
     */
    val port: Int,
    /**
     * The granularity of aggregations, in seconds.
     */
    val granularity: Int,
    /**
     * The time in seconds aggregated entries should persist after
     * their creation.
     */
    @JsonProperty("aggregation-ttl")
    val aggregationTtl: Int,
    /**
     * Whether or not timestamp fields should be validated for being
     * in the correct granularity when a message is received.
     */
    @JsonProperty("verify-timestamp")
    val verifyTimestamp: Boolean = true,
    /**
     * The connection information for the ZMQ plugin manager.
     */
    val pipeline: ZMQConfiguration = ZMQConfiguration("localhost:30101", "Aggregator")
) {
    /**
     * Ensures that this is a valid configuration, i.e. that all properties
     * have sensible values. Will throw an exception for values that are
     * incorrect.
     */
    fun validate(): Configuration {
        // Ensure this is a valid JDBC URL.
        if (!databaseUrl.startsWith("postgresql:")) {
            throw IllegalArgumentException("configuration.databaseUrl must start with `postgresql:`")
        }

        // Ensure that our port is valid.
        if (port <= 1 || port >= 65535) {
            throw IllegalArgumentException("configuration.port must be a valid port number <= 65535")
        }

        // Ensure that our granularity is somewhat reasonable.
        if (granularity <= 0) {
            throw IllegalArgumentException("configuration.granularity may not be negative")
        }

        // Ensure that our TTL is not negative or less than the granularity.
        if (aggregationTtl <= 0) {
            throw IllegalArgumentException("configuration.aggregationTtl may not be negative")
        }

        if (aggregationTtl <= granularity) {
            throw IllegalArgumentException(
                "configuration.aggregationTtl may not be less than the granularity:" +
                    "the database would always be empty!"
            )
        }

        return this
    }

    companion object {
        private val logger = mu.KotlinLogging.logger { }

        /**
         * Parses the configuration file located at the specified path into
         * a configuration of the content. Will throw if the config is not
         * formatted properly.
         * @param path the path to the configuration file
         * @returns the parsed configuration
         */
        fun load(path: Path): Configuration {
            logger.debug { "Loading configuration from $path" }
            val content = Files.readString(path)

            return parse(content).also {
                logger.debug { "Parsed configuration: $it" }
            }
        }

        /**
         * Parses a configuration object from the specified YAML string.
         * Will throw if the config is not formatted properly.
         * @param content the configuration as a YAML string
         * @returns the parsed configuration
         */
        fun parse(content: String): Configuration {
            val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
            mapper.registerModule(KotlinModule())

            return mapper.readValue(content, Configuration::class.java)
        }
    }
}

/**
 * Represents the connection information for the ZMQ plugin manager.
 */
data class ZMQConfiguration(
    /**
     * The path to the plugin manager, without tcp://.
     */
    @JsonProperty("manager-host")
    val managerHost: String,
    /**
     * The ID of this component, within the pipeline.
     */
    @JsonProperty("plugin-id")
    val id: String
)
