package nl.tudelft.hyperion.aggregator

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
        val databaseUrl: String,
        val port: Int,
        val granularity: Int
) {
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