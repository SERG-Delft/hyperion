package nl.tudelft.hyperion.pipeline.extractor

import nl.tudelft.hyperion.pipeline.readYAMLConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions
import java.io.File
import java.nio.file.Files

class ConfigurationTests {
    private val yaml = """
        pipeline :
          manager-host: "1.2.3.4:4567"
          plugin-id : "plugin1"
        
        fields:
          - field : "message"
            match : "\\[.+?\\] INFO [^:]+:(\\d+) (-) .+"
            extract :
              - to : "location.line"
                type : "number"
              - to : "dash"
                type : "string"
          - field: "message_2"
            match: "\\[.+?\\] INFO [^:]+:(\\d+) (-) .+"
            extract:
              - to: "location.line_2"
                type: "number"
              - to: "dash_2"
                type: "string"
    """.trimIndent()

    @Test
    fun parsePluginConfiguration() {
        val config = parseConfig(yaml)

        Assertions.assertEquals("plugin1", config.pipeline.id)
        Assertions.assertEquals("1.2.3.4:4567", config.pipeline.pluginManager)
    }

    @Test
    fun parseExtractorConfigurationFieldOneMatch() {
        val config = parseConfig(yaml)

        val extractableField1 = config.fields[0]
        Assertions.assertEquals("message", extractableField1.field)
        Assertions.assertEquals("""\[.+?\] INFO [^:]+:(\d+) (-) .+""", extractableField1.match)
    }

    @Test
    fun parseExtractorConfigurationFieldOneExtractionTypes() {
        val config = parseConfig(yaml)

        val extractableField1 = config.fields[0]
        Assertions.assertEquals("location.line", extractableField1.extract[0].to)
        Assertions.assertEquals("dash", extractableField1.extract[1].to)
        Assertions.assertEquals(Type.NUMBER, extractableField1.extract[0].type)
        Assertions.assertEquals(Type.STRING, extractableField1.extract[1].type)
    }

    @Test
    fun parseExtractorConfigurationFieldTwoMatch() {
        val config = parseConfig(yaml)

        val extractableField2 = config.fields[1]
        Assertions.assertEquals("message_2", extractableField2.field)
        Assertions.assertEquals("""\[.+?\] INFO [^:]+:(\d+) (-) .+""", extractableField2.match)
    }

    @Test
    fun parseExtractorConfigurationFieldTwoExtractionTypes() {
        val config = parseConfig(yaml)

        val extractableField2 = config.fields[1]
        Assertions.assertEquals("location.line_2", extractableField2.extract[0].to)
        Assertions.assertEquals("dash_2", extractableField2.extract[1].to)
        Assertions.assertEquals(Type.NUMBER, extractableField2.extract[0].type)
        Assertions.assertEquals(Type.STRING, extractableField2.extract[1].type)
    }
}

private fun parseConfig(content: String): Configuration {
    val tempFile = File.createTempFile("config", "yaml")
    Files.writeString(tempFile.toPath(), content)

    return readYAMLConfig(tempFile.toPath())
}
