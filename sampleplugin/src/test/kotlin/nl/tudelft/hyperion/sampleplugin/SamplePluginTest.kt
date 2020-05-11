package nl.tudelft.hyperion.sampleplugin

import nl.tudelft.hyperion.pluginmanager.RedisConfig
import nl.tudelft.hyperion.pluginmanager.hyperionplugin.PluginConfiguration
import nl.tudelft.hyperion.pluginmanager.hyperionplugin.SamplePlugin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SamplePluginTest() {
    @Test
    fun samplePluginWork() {
        // TODO: mock HyperionPlugin
        // TODO: fails on connect to redis
        val redisconfig = RedisConfig("redis", 6969)
        val config = PluginConfiguration(redisconfig, "-config", "test0r")
        val plugin = SamplePlugin(config)
        assertEquals( "test0r: [kip]", plugin.work("kip"))
    }
}
