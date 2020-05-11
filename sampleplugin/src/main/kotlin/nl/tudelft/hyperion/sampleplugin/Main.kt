@file:JvmName("Main")

package nl.tudelft.hyperion.sampleplugin

import nl.tudelft.hyperion.pluginmanager.hyperionplugin.SamplePlugin
import nl.tudelft.hyperion.pluginmanager.hyperionplugin.PluginConfiguration
import java.nio.file.Path

fun main(vararg args: String) {
    println("Loading config from ${args[0]}")
    val config = PluginConfiguration.load(Path.of(args[0]).toAbsolutePath())
    println("Starting plugin ${config.name}")
    SamplePlugin(config)

    println("Going to sleep in ${Thread.currentThread().name}")

    while (true) {
        Thread.sleep(Long.MAX_VALUE)
    }
}
