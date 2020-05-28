package nl.tudelft.hyperion.pipeline.pathextractor.configuration

import nl.tudelft.hyperion.pipeline.readYAMLConfig
import nl.tudelft.hyperion.pipeline.pathextractor.Configuration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class ConfigurationTests {
    private val yaml = """
            pipeline :
              manager-host: "1.2.3.4:4567"
              plugin-id : "plugin1"
            
            field: "log4j_file"
            relativePathFromSource: "src/main/java"
            postfix: .java
        """.trimIndent()

    @Test
    fun parsePluginConfiguration() {
        val config = parseConfig(yaml)

        Assertions.assertEquals("plugin1", config.pipeline.id)
        Assertions.assertEquals("1.2.3.4:4567", config.pipeline.pluginManager)
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
