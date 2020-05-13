package nl.tudelft.hyperion.pluginmanager

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PluginManagerTest() {

    private val redisConfig = RedisConfig("redis", 6969)
    private val plugins = listOf("Datasource", "Renamer", "Aggregator")
    private val config = Configuration(redisConfig, "-config", plugins)

    @Test
    fun `Fail pushConfig when sync fails`() {
        mockkStatic("io.lettuce.core.RedisClient")

        val client = mockk<RedisClient>(relaxed = true)
        every {
            RedisClient.create(any<RedisURI>())
        } returns client

        val conn = mockk<StatefulRedisConnection<String, String>>(relaxed = true)
        every {
            client.connect()
        } returns conn

        every {
            conn.sync()
        } throws Exception("Cannot sync with redis")

        val pluginManager = PluginManager(config)

        val exception: Exception = assertThrows("Cannot sync with redis") { pluginManager
            .pushConfig() }

        val expectedMessage = "Cannot sync with redis"
        val actualMessage = exception.message

        assertTrue(actualMessage!!.contains(expectedMessage))
    }


    @Test
    fun `Redis connection established in init`() {
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

    @Test
    fun `Register calls with two plugins`() {
        mockkStatic("io.lettuce.core.RedisClient")

        val client = mockk<RedisClient>(relaxed = true)

        every {
            RedisClient.create(any<RedisURI>())
        } returns client

        val plugins = listOf("Datasource", "Aggregator")
        val config = Configuration(redisConfig, null, plugins)
        val mock = spyk(PluginManager(config))

        mock.pushConfig()

        // verify that the right pub/sub calls were made
        verify(exactly = 1) {
            mock.registerPublish("Datasource", "Datasource-output")
            mock.registerSubscribe("Aggregator", "Datasource-output")
        }

        // verify that no more pub/sub calls were made
        verify(atMost = 1) {
            mock.registerPublish(any(), any())
            mock.registerSubscribe(any(), any())
        }
    }

    @Test
    fun `Register calls with three plugins`() {
        mockkStatic("io.lettuce.core.RedisClient")

        val client = mockk<RedisClient>(relaxed = true)

        every {
            RedisClient.create(any<RedisURI>())
        } returns client

        val mock = spyk(PluginManager(config))

        mock.pushConfig()

        verify(exactly = 1) {
            mock.registerPublish("Datasource", "Datasource-output")
            mock.registerSubscribe("Renamer", "Datasource-output")
            mock.registerPublish("Renamer", "Renamer-output")
            mock.registerSubscribe("Aggregator", "Renamer-output")
        }

        verify(atMost = 2) {
            mock.registerPublish(any(), any())
            mock.registerSubscribe(any(), any())
        }
    }

    @AfterEach
    internal fun tearDown() {
        clearAllMocks()
    }
}