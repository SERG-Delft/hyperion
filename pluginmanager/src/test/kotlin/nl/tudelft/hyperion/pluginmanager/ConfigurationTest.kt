package nl.tudelft.hyperion.pluginmanager

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals

class ConfigurationTest() {

    @Test
    fun `Load only one plugin`() {
        // load only one plugin which should be invalid
        val plugins = listOf(mapOf("name" to "Source", "host" to "localhost"))
        val config = Configuration("localhost", plugins)

        val exception: Exception = assertThrows("At least 2 plugins should be defined, got ${plugins.size}") { config.verify() }

        val expectedMessage = "At least 2 plugins should be defined, got ${plugins.size}"
        val actualMessage = exception.message

        Assertions.assertTrue(actualMessage!!.contains(expectedMessage))
    }

    @Test
    fun `Load config from file`() {
        val host = "tcp://localhost:5560"
        val plugins = listOf(
            mapOf("name" to "Datasource", "host" to "tcp://localhost:1200"),
            mapOf("name" to "Renamer", "host" to "tcp://localhost:1201"),
            mapOf("name" to "Aggregator", "host" to "tcp://localhost:1202")
        )

        val temporaryFile = File.createTempFile("hyperion-pluginmanager-config", "yml")
        Files.writeString(
            temporaryFile.toPath(), """
                host: "tcp://localhost:5560"
                plugins:
                  - name: "Datasource"
                    host: "tcp://localhost:1200"
                  - name: "Renamer"
                    host: "tcp://localhost:1201"
                  - name: "Aggregator"
                    host: "tcp://localhost:1202"
            """.trimIndent()
        )

        val config = Configuration.load(temporaryFile.toPath())
        config.verify()

        assertEquals(host, config.host)
        assertEquals(plugins, config.plugins)
    }
}