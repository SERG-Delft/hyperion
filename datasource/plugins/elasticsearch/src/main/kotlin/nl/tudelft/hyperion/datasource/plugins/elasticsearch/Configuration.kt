package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Represents a configuration setup for the Elasticsearch plugin.
 * Contains the necessary arguments to communicate and pull data from
 * Elasticsearch.
 */
class Configuration(
        val hostname: String,
        val index: String,
        val port: Int,
        val scheme: String,
        val timestampField: String,
        val pollInterval: Long,
        val responseHitMax: Int,
        val username: String?,
        val password: String?
) {

    /**
     * Verifies that the configuration is correct.
     */
    fun verify() {
        if (port !in 0..65535) {
            throw IllegalArgumentException("port must be between 0 and 65535 but was port=$port")
        }

        val schemeLower = scheme.toLowerCase()
        if (schemeLower != "http" && schemeLower != "https") {
            throw IllegalArgumentException("scheme must be 'http' or 'https' but not scheme=$schemeLower")
        }

        if (pollInterval < 1) {
            throw IllegalArgumentException("poll_interval must be a positive non-zero integer")
        }

        if (responseHitMax < 1) {
            throw IllegalArgumentException("response_hit_max must be a positive non-zero integer")
        }
    }

    companion object {
        /**
         * Parses the configuration file located at the specified path into
         * a configuration of the content. Will throw if the config is not
         * formatted properly.
         *
         * @param path the path to the configuration file
         * @return the parsed configuration
         */
        fun load(path: Path): Configuration {
            val content = Files.readString(path)
            return parse(content)
        }

        /**
         * Parses a configuration object from the specified YAML string.
         * Will throw if the config is not formatted properly.
         *
         * @param content the configuration as a YAML string
         * @return the parsed configuration
         */
        private fun parse(content: String): Configuration {
            val mapper = ObjectMapper(YAMLFactory())
            mapper.registerModule(KotlinModule())

            return mapper.readValue(content, Configuration::class.java)
                    .also(Configuration::verify)
        }
    }

}