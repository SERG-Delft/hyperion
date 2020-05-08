package nl.tudelft.hyperion.extractor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.nio.file.Files
import java.nio.file.Path

/**
 * Data class for naming extracted information
 * @param to : The name of the new field
 * @param type : type of the extracted value
 */
data class Extract(val to : String, val type : String)

/**
 * Data class for the configuration of the plugin
 * @param field : name of the field to match
 * @param match : regex to match the value of the field on
 * @param rename : renaming scheme
 */
class Configuration(
        val field : String,
        val match : String,
        val extract : List<Extract>
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
        private fun parse(content: String): Configuration {
            val mapper = ObjectMapper(YAMLFactory())
            mapper.registerModule(KotlinModule())

            return mapper.readValue(content, Configuration::class.java)
        }
    }
}
