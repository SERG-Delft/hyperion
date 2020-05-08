@file:JvmName("Main")

package nl.tudelft.hyperion.pluginmanager

import java.nio.file.Path

private val logger = mu.KotlinLogging.logger {}


/**
 * Main entry point for the pluginmanager. Loads the configuration,
 * pushes the plugin configuration to redis and exits.
 */
fun main(vararg args: String) {

    logger.info {"Loading config from ${args[0]}"}
    val config = try {
        val config = Configuration.load(Path.of(args[0]).toAbsolutePath())
        config.verify()
        config
    } catch (ex: Exception) {
        logger.error(ex) {"Failed to parse configuration. Does the file exist and is it valid YAML?"}
        return
    }

    logger.info {"Starting Plugin Manager"}
    try {
        PluginManager(config)
    } catch (ex: Exception) {
        logger.error(ex) {"Failed to execute Plugin Manager"}
        return
    }
    logger.info {"Started Plugin Manager"}
}
