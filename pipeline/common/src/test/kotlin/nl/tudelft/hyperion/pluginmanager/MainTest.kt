package nl.tudelft.hyperion.pluginmanager

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class MainTest() {
    @Test
    fun `Start PluginManager when right config`() {
        val redisConfig = RedisConfig("redis", 6969)
        val plugins = listOf("Source", "Sink")
        val config = Configuration(redisConfig, "-config", plugins)

        mockkObject(Configuration.Companion)

        every { Configuration.load(any())} returns config

        mockkConstructor(PluginManager::class)

        mockkStatic("io.lettuce.core.RedisClient")

        val client = mockk<RedisClient>(relaxed = true)

        every {
            RedisClient.create(any<RedisURI>())
        } returns client

        main("chicken")

        verify { anyConstructed<PluginManager>().pushConfig() }
    }

    @AfterEach
    internal fun tearDown() {
        clearAllMocks()
    }
}