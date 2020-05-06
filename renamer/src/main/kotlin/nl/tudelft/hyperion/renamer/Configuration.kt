package nl.tudelft.hyperion.renamer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.nio.file.Files
import java.nio.file.Path

class Rename(val from : String, val to : String)

/**
 * Configuration for Redis communication.
 *
 * @property host hostname of the Redis instance
 * @property portIn port of the Redis instance to subscribe to
 * @property channelIn name of the Redis channel to subscribe to
 * @property portOut port of the Redis instance to publish to
 * @property channelOut name of the Redis channel to publish to
 */
data class RedisConfig(
        val host: String,
        var portIn: Int?,
        val channelIn: String,
        var portOut: Int?,
        val channelOut: String
) {
    init {
        // set default values if field is missing
        // jackson does not support default value setting as of 2.7.1
        if (portIn == null) {
            portIn = 6380
        }

        if (portOut == null) {
            portOut = 6381
        }
    }
}

/**
 *
 */
class Configuration(
        val redis : RedisConfig,
        val rename : List<Rename>
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
