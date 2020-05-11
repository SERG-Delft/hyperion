package nl.tudelft.hyperion.hyperionplugin.common

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.RedisPubSubListener
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import nl.tudelft.hyperion.pluginmanager.hyperionplugin.PluginConfiguration

abstract class HyperionPlugin(private val config: PluginConfiguration) {
    private val logger = mu.KotlinLogging.logger {}

    var registered = false
    private val channelConfig = "${config.name}${config.registrationChannelPostfix}"

    var publisher = false
    lateinit var pubChannel: String
    lateinit var pub: StatefulRedisPubSubConnection<String, String>

    var subscriber = false
    lateinit var subChannel: String
    lateinit var sub: StatefulRedisPubSubConnection<String, String>

    val redisURI = RedisURI.create(config.redis.host, config.redis.port!!)
    lateinit var redisClient: RedisClient

    init {
        logger.info {"Starting HyperionPLugin-${config.name}"}
        register()
        logger.info {"HyperionPLugin-${config.name} enters infinte sleep on main thread"}
        // TODO: enable infinte sleep with logging
    }

    fun onMessage(message: String) {
        val ret: String = work(message)
        if (publisher) {
            val sync = pub.async()
            sync.publish(pubChannel, ret)
        }
    }

    private fun register() {
        redisClient = RedisClient.create(redisURI)

        val conn = redisClient.connect()
        val sync = conn.sync()

        subscriber = sync.hget(channelConfig, "subscriber")!!.toBoolean()
        publisher = sync.hget(channelConfig, "publisher")!!.toBoolean()

        if (publisher) {
            connectPublisher()
            logger.info {"${config.name} registered as publisher on: $pubChannel"}
        }
        if (subscriber) {
            connectSubscriber()
            logger.info {"${config.name} registered as subscriber on: $subChannel"}
        }
        registered = true
        logger.info {"${config.name} registered as pub: $publisher, sub: $subscriber"}
    }

    fun connectPublisher() {
        val conn = redisClient.connect()
        val sync = conn.sync()

        pubChannel = sync.hget(channelConfig, "pubChannel")
        pub = redisClient.connectPubSub()
    }

    fun connectSubscriber() {
        val conn = redisClient.connect()
        val sync = conn.sync()

        subChannel = sync.hget(channelConfig, "subChannel")
        sub = redisClient.connectPubSub()

        val listener: RedisPubSubListener<String, String> = object : RedisPubSubAdapter<String, String>() {
            override fun message(channel: String, message: String) {
                onMessage(message)
            }
        }
        sub.addListener(listener);
        val pssync = sub.sync()
        pssync.subscribe(subChannel)
    }

    abstract fun work(message: String): String
}
