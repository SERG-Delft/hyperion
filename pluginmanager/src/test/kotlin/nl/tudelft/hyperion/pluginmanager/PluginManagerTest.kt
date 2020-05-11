package nl.tudelft.hyperion.pluginmanager

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PluginManagerTest() {

    private val redisConfig = RedisConfig("redis", 6969)
    private val plugins = listOf("Datasource", "Renamer", "Aggregator")
    private val config = Configuration(redisConfig, "-config", plugins)


    @Test
    fun testRedisException() {
        val exception: Exception = assertThrows("Unable to connect to redis/<unresolved>:6969") { PluginManager(config) }

        val expectedMessage = "Unable to connect to redis/<unresolved>:6969"
        val actualMessage = exception.message

        assertTrue(actualMessage!!.contains(expectedMessage))
    }

    @Test
    fun testInit() {
        mockkStatic("io.lettuce.core.RedisClient")

        val client = mockk<RedisClient>(relaxed = true)

        every {
            RedisClient.create(any<RedisURI>())
        } returns client

        PluginManager(config)

        verify {
            client.connect()
        }
    }

    @AfterEach
    internal fun tearDown() {
        clearAllMocks()
    }
}