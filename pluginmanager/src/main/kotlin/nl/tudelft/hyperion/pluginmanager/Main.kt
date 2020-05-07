@file:JvmName("Main")

package nl.tudelft.hyperion.pluginmanager

import nl.tudelft.hyperion.pluginmanager.hyperionplugin.PluginConfiguration
import nl.tudelft.hyperion.pluginmanager.hyperionplugin.SamplePlugin
import java.nio.file.Path
import java.time.LocalDateTime


fun main(vararg args: String) {
    // order of plugins is as of now configured _in code_ in the PluginManager
    // use the onMessage function in the first plugin to put a message in the pipeline
    // tested with a fresh redis docker image

    // load config for Plugin Manager

    println("Loading config from ${args[0]}")
    val config = Configuration.load(Path.of(args[0]).toAbsolutePath())
    println("Starting Plugin Manager")
    PluginManager(config)

    // loading plugins
    /*
    val configp1 = PluginConfiguration.load(Path.of("sampleplugin1.yaml").toAbsolutePath())
    // PluginManager does not have refs to plugins in practice
    val p1 = SamplePlugin(configp1)

    // write message to pl
    val currentDateTime = LocalDateTime.now()
    println(currentDateTime)
    for (x in 0..1000) {
        p1.onMessage("$x")
    }
    */
}
