package nl.tudelft.hyperion.renamer.configuration

import nl.tudelft.hyperion.pipeline.PipelineRedisConfiguration
import nl.tudelft.hyperion.renamer.Configuration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ConfigurationTest {
    private val yaml = """
            name: "Plugin1"
            redis:
              host: "192.168.2.168"
              port: 6379
            
            rename :
              - from : "log_line"
                to : "location.line"
        """.trimIndent()

    @Test
    fun parseRedisConfiguration() {
        val config = Configuration.parse(yaml)

        // Redis Configuration
        Assertions.assertEquals("Plugin1", config.name)
        Assertions.assertEquals("192.168.2.168", config.redis.host)
        Assertions.assertEquals(6379, config.redis.port)
    }

    @Test
    fun parseRenameConfiguration() {
        val config = Configuration.parse(yaml)

        // Extraction Configuration
        Assertions.assertEquals("log_line", config.rename[0].from)
        Assertions.assertEquals("location.line", config.rename[0].to)
    }

    @Test
    fun channelPostfixGetSet() {
        val config = Configuration.parse(yaml)

        val expected = "postfix"
        config.registrationChannelPostfix = expected

        // Redis Configuration
        Assertions.assertEquals("postfix", config.registrationChannelPostfix)
    }

    @Test
    fun testRedisConfig() {
        val config = PipelineRedisConfiguration("localhost", 6379)

        Assertions.assertEquals("localhost", config.host)
        Assertions.assertEquals(6379, config.port)
    }
}