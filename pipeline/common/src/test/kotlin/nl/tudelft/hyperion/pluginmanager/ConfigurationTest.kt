package nl.tudelft.hyperion.pluginmanager

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ConfigurationTest() {

    @Test
    fun `Load only one plugin`() {
        // load only one plugin which should be invalid
        val plugins = listOf("Source")

        val redisConfig = RedisConfig("redis", 6969)
        val config = Configuration(redisConfig, "-config", plugins)

        val exception: Exception = assertThrows("At least two plugins should be provided, got 1") { config.verify() }

        val expectedMessage = "At least two plugins should be provided, got 1"
        val actualMessage = exception.message

        Assertions.assertTrue(actualMessage!!.contains(expectedMessage))
    }

    @Test
    fun `Load illegal redis port`() {
        // use redis port -1 which should be invalid
        val redisConfig = RedisConfig("redis", -1)

        val plugins = listOf("Source", "Sink")
        val config = Configuration(redisConfig, "-config", plugins)

        val exception: Exception = assertThrows("Redis port must be between 0 and 65535 but was -1") { config.verify() }

        val expectedMessage = "Redis port must be between 0 and 65535 but was -1"
        val actualMessage = exception.message

        Assertions.assertTrue(actualMessage!!.contains(expectedMessage))
    }

    @Test
    fun `Load null as redis port`() {
        // load port is null for redis config
        val redisConfig = RedisConfig("redis", null)

        val plugins = listOf("Source", "Sink")
        val config = Configuration(redisConfig, "-config", plugins)
        config.verify()

        assertEquals(6739, config.redis.port)
    }

    @Test
    fun `Load null as registrationChannelPostifx`() {
        // load port is null for redis config
        val redisConfig = RedisConfig("redis", 6969)

        val plugins = listOf("Source", "Sink")
        val config = Configuration(redisConfig, null, plugins)

        assertEquals("-config", config.registrationChannelPostfix)
    }
}