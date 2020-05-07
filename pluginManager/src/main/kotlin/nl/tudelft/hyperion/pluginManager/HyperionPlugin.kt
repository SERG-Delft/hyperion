package nl.tudelft.hyperion.pluginmanager

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.pubsub.RedisPubSubAdapter
import io.lettuce.core.pubsub.RedisPubSubListener
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection


abstract class HyperionPlugin(val name: String) {
    var registered = false
    private val channelConfig = "$name-config"

    var publisher = false
    lateinit var pubChannel: String
    lateinit var pub: StatefulRedisPubSubConnection<String, String>

    var subscriber = false
    lateinit var subChannel: String
    lateinit var sub: StatefulRedisPubSubConnection<String, String>

    lateinit var redisURI: RedisURI
    lateinit var redisClient: RedisClient

    init {
        println("Starting HyperionPLugin-$name")
        // TODO: read the yaml config file
        register()
    }

    fun onMessage(message: String) {
        val ret: String = work(message)
        if (publisher) {
            val sync = pub.sync()
            sync.publish(pubChannel, ret)
        }
    }

    private fun register() {
        redisURI = RedisURI.create("192.168.2.168", 6379)
        redisClient = RedisClient.create(redisURI)

        val conn = redisClient.connect()
        val sync = conn.sync()

        subscriber = sync.hget(channelConfig, "subscriber")!!.toBoolean()
        publisher = sync.hget(channelConfig, "publisher")!!.toBoolean()

        if (publisher) {
            connectPublisher()
        }
        if (subscriber) {
            connectSubscriber()
        }
        registered = true
        println("$name registered as pub: $publisher, sub: $subscriber")
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