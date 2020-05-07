package nl.tudelft.hyperion.pluginmanager

import io.lettuce.core.*
import io.lettuce.core.api.StatefulRedisConnection

class PluginManager(redisURI: RedisURI) {
    val channelConfig = "-config"
    val cm = ConnectionManager(redisURI)
    lateinit var plugins: MutableList<String>

    // TODO: use logging instead of printing
    init {
        readPlugins()
        configPlugins()
        println("Written config to redis")
    }

    private fun configPlugins() {
        /* write plugin info to redis
        * hget plugin-config subscriber
        * hget plugin-config publisher
        * hget plugin-config subChannel
        * hget plugin-config pubChannel */
        val pluginIterator = plugins.iterator()
        var subChannel = "${plugins[1]}-output"
        for ((index, plugin) in pluginIterator.withIndex()) {
            var pubChannel = "$plugin-output"
            when (index) {
                0 -> {
                    cm.hset("$plugin$channelConfig", mapOf("subscriber" to "false"))
                    registerPublish(plugin, pubChannel)
                }
                plugins.size - 1 -> {
                    registerSubscribe(plugin, subChannel)
                    cm.hset("$plugin$channelConfig", mapOf("publisher" to "false"))
                }
                else -> {
                    registerSubscribe(plugin, subChannel)
                    registerPublish(plugin, pubChannel)
                }
            }
            subChannel = pubChannel
        }
    }

    private fun registerPublish(plugin: String, channel: String) {
        cm.hset("$plugin$channelConfig", mapOf("publisher" to "true", "pubChannel" to channel))
    }

    private fun registerSubscribe(plugin: String, channel: String) {
        cm.hset("$plugin$channelConfig", mapOf("subscriber" to "true", "subChannel" to channel))
    }

    private fun readPlugins() {
        // determine order of plugins in pipeline
        // TODO: Make this read a yaml file
        // ?TODO: Make pipeline DAG
        plugins = mutableListOf("Plugin1", "Plugin2", "Plugin3", "Aggregator")
    }
}
