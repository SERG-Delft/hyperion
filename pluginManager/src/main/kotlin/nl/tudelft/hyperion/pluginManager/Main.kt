@file:JvmName("Main")

package nl.tudelft.hyperion.pluginmanager

import io.lettuce.core.RedisURI
import java.time.LocalDateTime


fun main(vararg args: String) {
    // order of plugins is as of now configured _in code_ in the PluginManager
    // use the onMessage function in the first plugin to put a message in the pipeline
    // tested with a fresh redis docker image

    println("Starting Plugin Manager")
    val pm = PluginManager(RedisURI.create("192.168.2.168", 6379))

    // PluginManager does not have refs to plugins in practice
    val p1 = SamplePlugin("Plugin1")
    val p2 = SamplePlugin("Plugin2")
    val p3 = SamplePlugin("Plugin3")
    val p4 = SamplePlugin("Aggregator")

    // write message to pl
    val currentDateTime = LocalDateTime.now()
    println(currentDateTime)
    for (x in 0..10000) {
        p1.onMessage("$x")
    }
}
