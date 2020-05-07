package nl.tudelft.hyperion.pluginmanager

import io.lettuce.core.*

class PluginManager(config: Configuration) {
    private val channelConfig = config.registrationChannelPostfix
    private val cm = ConnectionManager(RedisURI.create(config.redis.host, config.redis.port!!))
    private val plugins = config.plugins

    // TODO: use logging instead of printing
    init {
        configPlugins()
        println("Written config to redis")
    }

    private fun configPlugins() {
        /* write plugin info to redis
        * hget $plugin-config subscriber
        * hget $plugin-config publisher
        * hget $plugin-config subChannel
        * hget $plugin-config pubChannel */
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
}
