package nl.tudelft.hyperion.pipeline.renamer

import nl.tudelft.hyperion.pipeline.readYAMLConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class ConfigurationTest {
    private val yaml = """
            plugin:
                pluginManager: "1.2.3.4:4567"
                id: "plugin1"
            
            rename:
              - from: "log_line"
                to: "location.line"
        """.trimIndent()

    @Test
    fun parsePluginConfiguration() {
        val config = parseConfig(yaml)

        Assertions.assertEquals("plugin1", config.plugin.id)
        Assertions.assertEquals("1.2.3.4:4567", config.plugin.pluginManager)
    }

    @Test
    fun parseRenameConfiguration() {
        val config = parseConfig(yaml)

        Assertions.assertEquals("log_line", config.rename[0].from)
        Assertions.assertEquals("location.line", config.rename[0].to)
    }
}

private fun parseConfig(content: String): Configuration {
    val tempFile = File.createTempFile("config", "yaml")
    Files.writeString(tempFile.toPath(), content)

    return readYAMLConfig(tempFile.toPath())
}
