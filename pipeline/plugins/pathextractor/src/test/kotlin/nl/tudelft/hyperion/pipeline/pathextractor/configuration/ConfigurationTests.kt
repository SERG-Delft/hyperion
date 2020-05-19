package nl.tudelft.hyperion.pipeline.pathextractor.configuration

import nl.tudelft.hyperion.pipeline.readYAMLConfig
import nl.tudelft.hyperion.pipeline.pathextractor.Configuration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class ConfigurationTests {
    private val yaml = """
            plugin :
              pluginManager: "1.2.3.4:4567"
              id : "plugin1"
            
            field: "log4j_file"
            relativePathFromSource: "src/main/java"
            postfix: .java
        """.trimIndent()

    @Test
    fun parsePluginConfiguration() {
        val config = parseConfig(yaml)

        Assertions.assertEquals("plugin1", config.plugin.id)
        Assertions.assertEquals("1.2.3.4:4567", config.plugin.pluginManager)
    }

    @Test
    fun parsePathConfiguration() {
        val config = parseConfig(yaml)

        Assertions.assertEquals("log4j_file", config.field)
        Assertions.assertEquals("src/main/java", config.relativePathFromSource)
        Assertions.assertEquals(".java", config.postfix)
    }
}

private fun parseConfig(content: String): Configuration {
    val tempFile = File.createTempFile("config", "yaml")
    Files.writeString(tempFile.toPath(), content)

    return readYAMLConfig(tempFile.toPath())
}
