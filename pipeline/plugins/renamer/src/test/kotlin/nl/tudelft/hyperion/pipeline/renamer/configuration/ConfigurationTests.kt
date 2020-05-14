package nl.tudelft.hyperion.pipeline.renamer.configuration

import nl.tudelft.hyperion.pipeline.readYAMLConfig
import nl.tudelft.hyperion.pipeline.renamer.Configuration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class ConfigurationTest {
    private val yaml = """
            plugin:
                pluginManager: "1.2.3.4:4567"
                id: "Plugin1"
            
            rename:
              - from: "log_line"
                to: "location.line"
        """.trimIndent()

    @Test
    fun parseRedisConfiguration() {
        val config = parseConfig(yaml)

        // Plugin Configuration
        Assertions.assertEquals("Plugin1", config.plugin.id)
        Assertions.assertEquals("1.2.3.4:4567", config.plugin.pluginManager)
    }

    @Test
    fun parseRenameConfiguration() {
        val config = parseConfig(yaml)

        // Extraction Configuration
        Assertions.assertEquals("log_line", config.rename[0].from)
        Assertions.assertEquals("location.line", config.rename[0].to)
    }
}

private fun parseConfig(content: String): Configuration {
    val tempFile = File.createTempFile("config", "yaml")
    Files.writeString(tempFile.toPath(), content)

    return readYAMLConfig(tempFile.toPath())
}
