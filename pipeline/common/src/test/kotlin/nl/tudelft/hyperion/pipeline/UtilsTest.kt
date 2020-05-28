package nl.tudelft.hyperion.pipeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class UtilsTest {
    @Test
    fun `Parsing valid json string`() {
        val json = """{"test":"affirmative"}"""

        val m = readJSONContent<MutableMap<String, String>>(json)

        assertEquals("affirmative", m["test"])
    }

    @Test
    fun `Read YAML config from file`() {
        val temporaryFile = File.createTempFile("pipeline-plugin-config", "yml")
        Files.writeString(
            temporaryFile.toPath(), """
                id: "Plugin"
                pluginManager: "tcp://localhost:500"
            """.trimIndent()
        )

        val config = readYAMLConfig<MutableMap<String, String>>(temporaryFile.toPath())

        assertEquals("Plugin", config["id"])
    }
}