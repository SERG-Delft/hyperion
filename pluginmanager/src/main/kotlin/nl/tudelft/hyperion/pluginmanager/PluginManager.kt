package nl.tudelft.hyperion.pluginmanager

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection

@Suppress("TooGenericExceptionCaught")
class PluginManager(config: Configuration) {
    private val channelConfig = config.registrationChannelPostfix

    private val redisURI = RedisURI.create(config.redis.host, config.redis.port!!)
    private val redisClient = RedisClient.create(redisURI)
    private val conn: StatefulRedisConnection<String, String> = redisClient.connect()

    private val plugins = config.plugins

    private val logger = mu.KotlinLogging.logger {}

    init {
        logger.info {"Write plugin config to redis"}
        try {
            configPlugins()
        } catch (ex: Exception) {
            logger.error(ex) {"Failed to push plugin config to redis"}
        } finally {
            closeConnection()
            logger.debug {"Closed redis connection"}
        }
        logger.info {"Written config to redis"}
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
                    hset("$plugin$channelConfig", mapOf("subscriber" to "false"))
                    registerPublish(plugin, pubChannel)
                }
                plugins.size - 1 -> {
                    registerSubscribe(plugin, subChannel)
                    hset("$plugin$channelConfig", mapOf("publisher" to "false"))
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
        hset("$plugin$channelConfig", mapOf("publisher" to "true", "pubChannel" to channel))
    }

    private fun registerSubscribe(plugin: String, channel: String) {
        hset("$plugin$channelConfig", mapOf("subscriber" to "true", "subChannel" to channel))
    }

    private fun hset(key: String, value: Map<String, String>) {
        val sync = conn.sync()

        sync.hset(key, value)
    }

    private fun closeConnection() {
        conn.close()
        redisClient.shutdown()
    }
}
