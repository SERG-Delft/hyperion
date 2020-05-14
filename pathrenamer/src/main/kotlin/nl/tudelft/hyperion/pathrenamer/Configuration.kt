package nl.tudelft.hyperion.extractor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import nl.tudelft.hyperion.pipeline.PipelineRedisConfiguration
import java.nio.file.Files
import java.nio.file.Path

<<<<<<< HEAD:pluginmanager/src/main/kotlin/nl/tudelft/hyperion/pluginmanager/Configuration.kt
// TODO: add comments explaining config options
data class RedisConfig(
    val host: String,
    var port: Int?
) {
    init {
        if (port == null) {
            port = 6739
        }
    }
}
=======
/**
 * Data class for naming extracted information
 * @param to : The name of the new field
 * @param type : type of the extracted value
 */
data class Extract(val to : String, val type : String)
>>>>>>> extraction-plugin-init:extractor/src/main/kotlin/nl/tudelft/hyperion/extractor/Configuration.kt

/**
 * Data class for the configuration of the plugin
 * @param field : name of the field to match
 * @param match : regex to match the value of the field on
 * @param extract : naming scheme
 */
data class Configuration(
<<<<<<< HEAD:pluginmanager/src/main/kotlin/nl/tudelft/hyperion/pluginmanager/Configuration.kt
    val redis: RedisConfig,
    var registrationChannelPostfix: String?,
    var plugins: List<String>
=======
        val field : String,
        val match : String,
        val extract : List<Extract>,
        val redis: PipelineRedisConfiguration,
        var registrationChannelPostfix: String?,
        val name: String
>>>>>>> extraction-plugin-init:extractor/src/main/kotlin/nl/tudelft/hyperion/extractor/Configuration.kt
) {
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
        }
    }
}
