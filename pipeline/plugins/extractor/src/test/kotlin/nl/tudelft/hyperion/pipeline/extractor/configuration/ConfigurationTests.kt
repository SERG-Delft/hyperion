package nl.tudelft.hyperion.pipeline.extractor.configuration

import nl.tudelft.hyperion.pipeline.extractor.Configuration
import nl.tudelft.hyperion.pipeline.readYAMLConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions
import java.io.File
import java.nio.file.Files

class ConfigurationTests {
    private val yaml = """
        plugin :
          pluginManager: "1.2.3.4:4567"
          id : "plugin1"
        
        field : "message"
        match : "\\[.+?\\] INFO [^:]+:(\\d+) (-) .+"
        extract :
          - to : "location.line"
            type : "number"
          - to : "dash"
            type : "string"
    """.trimIndent()

    @Test
    fun parsePluginConfiguration() {
        val config = parseConfig(yaml)

        Assertions.assertEquals("plugin1", config.plugin.id)
        Assertions.assertEquals("1.2.3.4:4567", config.plugin.pluginManager)
    }

    @Test
    fun parseExtractorConfiguration() {
        val config = parseConfig(yaml)
        Assertions.assertEquals("message", config.field)
        Assertions.assertEquals("""\[.+?\] INFO [^:]+:(\d+) (-) .+""", config.match)
        Assertions.assertEquals("location.line", config.extract[0].to)
        Assertions.assertEquals("dash", config.extract[1].to)
        Assertions.assertEquals("number", config.extract[0].type)
        Assertions.assertEquals("string", config.extract[1].type)
    }
}

private fun parseConfig(content: String): Configuration {
    val tempFile = File.createTempFile("config", "yaml")
    Files.writeString(tempFile.toPath(), content)

    return readYAMLConfig(tempFile.toPath())
}
