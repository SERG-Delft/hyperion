package nl.tudelft.hyperion.renamer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import nl.tudelft.hyperion.pluginmanager.hyperionplugin.PluginConfiguration
import java.nio.file.Files
import java.nio.file.Path

class Rename(val from : String, val to : String)

/**
 * Configuration for renaming plugin
 * @param rename the list of renaming schemes
 */
data class Configuration(
        val rename : List<Rename>,
        val pluginConfig: PluginConfiguration
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
