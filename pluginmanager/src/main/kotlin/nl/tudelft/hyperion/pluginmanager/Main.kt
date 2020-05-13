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
    val config = Configuration.load(Path.of("pluginmanager/pluginmanager.yml").toAbsolutePath())
    print(config)

    //
    // logger.info {"Loading config from ${args[0]}"}
    // val config: Configuration? = try {
    //     val config = Configuration.load(Path.of(args[0]).toAbsolutePath())
    //     config.verify()
    //     config
    // } catch (ex: FileSystemException) {
    //     logger.error(ex) {"Failed to retrieve configuration file at ${args[0]}"}
    //     throw ex
    // } catch (ex: IllegalArgumentException) {
    //     logger.error(ex) {"Failed to parse config file"}
    //     throw ex
    // }
    //
    // logger.info {"Starting Plugin Manager"}
    // try {
    //     if (config != null) {
    //         val pluginManager = PluginManager(config)
    //         pluginManager.pushConfig()
    //     }
    // } catch (ex: Exception) {
    //     logger.error(ex) {"Failed to execute Plugin Manager"}
    //     throw ex
    // }
    // logger.info {"Started Plugin Manager"}
}
