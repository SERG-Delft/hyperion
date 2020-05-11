@file:JvmName("Main")

package nl.tudelft.hyperion.pluginmanager

import java.nio.file.Path

private val logger = mu.KotlinLogging.logger {}


/**
 * Main entry point for the pluginmanager. Loads the configuration,
 * pushes the plugin configuration to redis and exits.
 */
@Suppress("TooGenericExceptionCaught")
fun main(vararg args: String) {

    logger.info {"Loading config from ${args[0]}"}
    val config: Configuration? = try {
        val config = Configuration.load(Path.of(args[0]).toAbsolutePath())
        config.verify()
        config
    } catch (ex: FileSystemException) {
        logger.error(ex) {"Failed to retrieve configuration file at ${args[0]}"}
        null
    } catch (ex: IllegalArgumentException) {
        logger.error(ex) {"Failed to parse config file"}
        null
    }

    logger.info {"Starting Plugin Manager"}
    try {
        if (config != null) {
            PluginManager(config)
        }
    } catch (ex: Exception) {
        logger.error(ex) {"Failed to execute Plugin Manager"}
        return
    }
    logger.info {"Started Plugin Manager"}
}
