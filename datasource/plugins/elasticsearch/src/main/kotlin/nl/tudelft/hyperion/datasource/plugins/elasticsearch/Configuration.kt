package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.nio.file.Files
import java.nio.file.Path

/**
 * Configuration for communication with the manager.
 *
 * @property id id of this plugin
 * @property host hostname of PluginManager
 * @property bufferSize buffer size of the queues
 */
data class PipelineConfig(
    @JsonProperty("plugin-id")
    val id: String,
    @JsonProperty("manager-host")
    val host: String,
    @JsonProperty("buffer-size")
    val bufferSize: Int = 20_000
)

/**
 * Configuration for Elasticsearch communication.
 *
 * @property hostname hostname of the Elasticsearch server
 * @property index which index to retrieve documents from
 * @property port port of the Elasticsearch server
 * @property scheme scheme to use for retrieval, is either `http` or `https`
 * @property authentication whether authentication is enabled
 * @property timestampField which field to use for querying based on time
 * @property responseHitCount amount of hits to expect
 * @property username username to pass if authentication is set to true
 * @property password password to pass if authentication is set to true
 */
data class ElasticsearchConfig(
    val hostname: String,
    val index: String,
    var port: Int = 9200,
    var scheme: String = "http",
    var authentication: Boolean,
    @JsonProperty("timestamp-field")
    val timestampField: String,
    @JsonProperty("response-hit-count")
    var responseHitCount: Int,
    var username: String?,
    var password: String?
) {
    /**
     * Verifies that the configuration is correct.
     *
     * @throws IllegalArgumentException if any of the fields are invalid
     */
    fun verify() {
        if (port !in 0..65535) {
            throw IllegalArgumentException("port must be between 0 and 65535 but was port=$port")
        }

        val schemeLower = scheme.toLowerCase()
        if (schemeLower != "http" && schemeLower != "https") {
            throw IllegalArgumentException("scheme must be 'http' or 'https' but not scheme=$schemeLower")
        }

        if (responseHitCount < 1) {
            throw IllegalArgumentException("response_hit_count must be a positive non-zero integer")
        }

        if (authentication && (username == null || password == null)) {
            throw IllegalArgumentException("username and password must be provided to use authentication")
        }
    }
}

/**
 * Represents a configuration setup for the Elasticsearch plugin.
 * Contains the necessary arguments to communicate and pull data from
 * Elasticsearch.
 *
 * @property pollInterval time between sending queries in seconds
 * @property es Elasticsearch configuration
 */
data class Configuration(
    @JsonProperty("poll-interval")
    var pollInterval: Int,
    val pipeline: PipelineConfig,
    @JsonProperty("elasticsearch")
    val es: ElasticsearchConfig
) {

    /**
     * Verifies that the configuration is correct.
     *
     * @throws IllegalArgumentException if any of the fields are invalid
     */
    fun verify() {
        es.verify()

        if (pollInterval < 1) {
            throw IllegalArgumentException("poll_interval must be a positive non-zero integer")
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
        fun parse(content: String): Configuration {
            val mapper = ObjectMapper(YAMLFactory())
            mapper.registerModule(KotlinModule())

            return mapper.readValue(content, Configuration::class.java)
                .also(Configuration::verify)
        }
    }
}
