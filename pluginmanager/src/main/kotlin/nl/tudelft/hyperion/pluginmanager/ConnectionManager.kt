package nl.tudelft.hyperion.pluginmanager

import io.lettuce.core.*
import io.lettuce.core.api.StatefulRedisConnection

private val logger = mu.KotlinLogging.logger {}

class ConnectionManager(redisURI: RedisURI) {
    // this is currently only used by the PluginManager
    // TODO: handle generic types of connection
    lateinit var redisClient: RedisClient
    lateinit var conn: StatefulRedisConnection<String, String>

    init {
        logger.debug {"Setting up redis connection"}
        redisConnect(redisURI)
        logger.debug {"Redis connection setup"}
    }

    fun setKey(key: String, value: String) {
        val sync = conn.sync()

        sync.set(key, value)
    }

    fun hset(key: String, value: Map<String, String>) {
        val sync = conn.sync()

        sync.hset(key, value)
    }

    fun hget(key: String, field: String): String {
        val sync = conn.sync()

        return sync.hget(key, field)
    }

    fun closeConnection() {
        conn.close()
        redisClient.shutdown()
    }

    private fun redisConnect(redisURI: RedisURI) {
        redisClient = RedisClient.create(redisURI)
        conn = redisClient.connect()
    }
}
