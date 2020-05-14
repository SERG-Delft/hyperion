package nl.tudelft.hyperion.extractor.configuration

import nl.tudelft.hyperion.extractor.Configuration
import nl.tudelft.hyperion.pipeline.PipelineRedisConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions

class ConfigurationTests {
    @Test
    fun parseRedisConfiguration() {
        val yaml = """
            name: "Plugin1"
            redis:
              host: "192.168.2.168"
              port: 6379

            field : "message"
            match : "\\[.+?\\] INFO [^:]+:(\\d+) (-) .+"
            extract :
              - to : "location.line"
                type : "number"
              - to : "dash"
                type : "string"
        """.trimIndent()

        val config = Configuration.parse(yaml)

        // Redis Configuration
        Assertions.assertEquals("Plugin1", config.name)
        Assertions.assertEquals("192.168.2.168", config.redis.host)
        Assertions.assertEquals(6379, config.redis.port)
    }

    @Test
    fun parseExtractionConfiguration() {
        val yaml = """
            name: "Plugin1"
            redis:
              host: "192.168.2.168"
              port: 6379
            
            field : "message"
            match : "\\[.+?\\] INFO [^:]+:(\\d+) (-) .+"
            extract :
              - to : "location.line"
                type : "number"
              - to : "dash"
                type : "string"
        """.trimIndent()

        val config = Configuration.parse(yaml)

        // Extraction Configuration
        Assertions.assertEquals("message", config.field)
        Assertions.assertEquals("\\[.+?\\] INFO [^:]+:(\\d+) (-) .+", config.match)
        Assertions.assertEquals("location.line", config.extract[0].to)
        Assertions.assertEquals("number", config.extract[0].type)
        Assertions.assertEquals("dash", config.extract[1].to)
        Assertions.assertEquals("string", config.extract[1].type)
    }

    @Test
    fun channelPostfixGetSet() {
        val yaml = """
            name: "Plugin1"
            redis:
              host: "192.168.2.168"
              port: 6379
            
            field : "message"
            match : "\\[.+?\\] INFO [^:]+:(\\d+) (-) .+"
            extract :
              - to : "location.line"
                type : "number"
              - to : "dash"
                type : "string"
        """.trimIndent()

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
