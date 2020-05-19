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
    logger.info { "Loading config from ${args[0]}" }
    val config = Configuration.load(Path.of(args[0]).toAbsolutePath()).also(Configuration::verify)

    logger.info { "Starting PluginManager" }
    try {
        val pluginManager = PluginManager(config)
        pluginManager.launchListener()
    } catch (ex: Exception) {
        logger.error(ex) { "Failed to execute PluginManager" }
        throw ex
    }

    logger.info { "Stopped PluginManager" }
}
