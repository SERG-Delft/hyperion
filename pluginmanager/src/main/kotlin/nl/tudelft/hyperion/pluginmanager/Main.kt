@file:JvmName("Main")

package nl.tudelft.hyperion.pluginmanager

import java.nio.file.Path

private val logger = mu.KotlinLogging.logger {}


/**
 * Main entry point for the :PluginManager:. Loads the configuration,
 * Initializes the :PluginManager: and launches listener for register requests.
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
        throw ex
    } catch (ex: IllegalArgumentException) {
        logger.error(ex) {"Failed to parse config file"}
        throw ex
    }

    logger.info {"Starting PluginManager"}
    try {
        if (config != null) {
            val pluginManager = PluginManager(config)
            pluginManager.launchListener()
        }
    } catch (ex: Exception) {
        logger.error(ex) {"Failed to execute PluginManager"}
        throw ex
    }
    logger.info {"Stopped PluginManager"}
}
