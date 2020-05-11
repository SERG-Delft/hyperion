@file:JvmName("Main")

package nl.tudelft.hyperion.hyperionplugin.plugins.sampleplugin

import nl.tudelft.hyperion.pluginmanager.hyperionplugin.PluginConfiguration
import java.nio.file.Path

private val logger = mu.KotlinLogging.logger {}

fun main(vararg args: String) {

    logger.info{"Loading config from ${args[0]}"}
    val config = PluginConfiguration.load(Path.of(args[0]).toAbsolutePath())
    logger.info{"Starting plugin ${config.name}"}
    SamplePlugin(config)

    logger.info{"Going to sleep in ${Thread.currentThread().name}"}

    while (true) {
        Thread.sleep(Long.MAX_VALUE)
    }
}
