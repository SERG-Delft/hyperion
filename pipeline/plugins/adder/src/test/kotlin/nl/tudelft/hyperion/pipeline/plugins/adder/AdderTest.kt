package nl.tudelft.hyperion.pipeline.plugins.adder

import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import nl.tudelft.hyperion.pipeline.readYAMLConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class AdderTest {
    private val pipeline = PipelinePluginConfiguration("test-adder", "localhost")

    @Test
    fun `add one parent level field`() {
        val config = listOf(
            AddConfiguration("version", "1.0.2")
        )
        val adder = AdderPlugin(AdderConfiguration(pipeline, config))

        val input = """{"message":"hey"}"""
        val expected = """{"message":"hey","version":"1.0.2"}"""

        val ret = runBlocking { adder.process(input) }
        Assertions.assertEquals(expected, ret)
    }


    @Test
    fun `return normal input when unable to parse`() {
        val config = listOf(
            AddConfiguration("version", "1.0.2")
        )
        val adder = AdderPlugin(AdderConfiguration(pipeline, config))

        val input = """chicken"""
        val expected = """chicken"""

        val ret = runBlocking { adder.process(input) }
        Assertions.assertEquals(expected, ret)
    }

    @Test
    fun `add two parent level fields`() {
        val config = listOf(
            AddConfiguration("version", "1.0.2"),
            AddConfiguration("secret", "egg")
        )
        val adder = AdderPlugin(AdderConfiguration(pipeline, config))

        val input = """{"message":"hey"}"""
        val expected = """{"message":"hey","version":"1.0.2","secret":"egg"}"""

        val ret = runBlocking { adder.process(input) }
        Assertions.assertEquals(expected, ret)
    }

    @Test
    fun `value null of existing key should be overriden on default`() {
        val config = listOf(
            AddConfiguration("version", "1.0.2")
        )
        val adder = AdderPlugin(AdderConfiguration(pipeline, config))

        val input = """{"version":null}"""
        val expected = """{"version":"1.0.2"}"""

        val ret = runBlocking { adder.process(input) }
        Assertions.assertEquals(expected, ret)
    }

    @Test
    fun `value null of existing key should not be overriden when false`() {
        val config = listOf(
            AddConfiguration("version", "1.0.2", false)
        )
        val adder = AdderPlugin(AdderConfiguration(pipeline, config))

        val input = """{"version":null}"""
        val expected = """{"version":null}"""

        val ret = runBlocking { adder.process(input) }
        Assertions.assertEquals(expected, ret)
    }

    @Test
    fun `add two same key parent level fields shouldn't overwrite`() {
        val config = listOf(
            AddConfiguration("version", "1.0.2"),
            AddConfiguration("version", "2.1")
        )
        val adder = AdderPlugin(AdderConfiguration(pipeline, config))

        val input = """{"message":"hey"}"""
        val expected = """{"message":"hey","version":"1.0.2"}"""

        val ret = runBlocking { adder.process(input) }
        Assertions.assertEquals(expected, ret)
    }

    @Test
    fun `add new field 1 level deep`() {
        val config = listOf(
            AddConfiguration("code.version", "1.0.2")
        )
        val adder = AdderPlugin(AdderConfiguration(pipeline, config))

        val input = """{"message":"hey"}"""
        val expected = """{"message":"hey","code":{"version":"1.0.2"}}""".trimMargin()

        val ret = runBlocking { adder.process(input) }
        Assertions.assertEquals(expected, ret)
    }

    @Test
    fun `add field to existing 1 level deep`() {
        val config = listOf(
            AddConfiguration("code.version", "1.0.2")
        )
        val adder = AdderPlugin(AdderConfiguration(pipeline, config))

        val input = """{"code":{"author":"superman"}}"""
        val expected = """{"code":{"author":"superman","version":"1.0.2"}}"""

        val ret = runBlocking { adder.process(input) }
        Assertions.assertEquals(expected, ret)
    }

    @Test
    fun `add same field to existing 1 level deep shouldn't overwrite`() {
        val config = listOf(
            AddConfiguration("code.author", "batman")
        )
        val adder = AdderPlugin(AdderConfiguration(pipeline, config))

        val input = """{"code":{"author":"superman"}}"""
        val expected = """{"code":{"author":"superman"}}"""

        val ret = runBlocking { adder.process(input) }
        Assertions.assertEquals(expected, ret)
    }

    @Test
    fun `update configuration`() {
        val temporaryFile = File.createTempFile("pipeline-adder-config", "yaml")
        Files.writeString(
            temporaryFile.toPath(), """
                pipeline:
                    manager-host: localhost
                    plugin-id: test-adder
                add:
                    - key: version
                      value: 2
            """.trimIndent()
        )

        val config = listOf(
            AddConfiguration("version", "1")
        )
        val adder = AdderPlugin(AdderConfiguration(pipeline, config))

        Assertions.assertEquals("1", adder.config.add.get(0).value)
        adder.updateConfig(temporaryFile.absolutePath)
        Assertions.assertEquals("2", adder.config.add.get(0).value)
    }

    @Test
    fun `updated config is used in process`() {
        val temporaryFile = File.createTempFile("pipeline-adder-config", "yaml")
        Files.writeString(
            temporaryFile.toPath(), """
                pipeline:
                    manager-host: localhost
                    plugin-id: test-adder
                add:
                    - key: version
                      value: 2
            """.trimIndent()
        )

        val config = listOf(
            AddConfiguration("version", "1")
        )
        val adder = AdderPlugin(AdderConfiguration(pipeline, config))

        var ret = runBlocking { adder.process("""{}""") }
        Assertions.assertEquals("""{"version":"1"}""", ret)

        adder.updateConfig(temporaryFile.absolutePath)
        ret = runBlocking { adder.process("""{}""") }
        Assertions.assertEquals("""{"version":"2"}""", ret)
    }

    @Test
    fun `change of file should update config`() {
        val temporaryFile = File.createTempFile("hyperion-add-config", "yaml")
        Files.writeString(
            temporaryFile.toPath(), """
                pipeline:
                    manager-host: localhost:39181
                    plugin-id: rate
                add:
                    - key: version
                      value: 1
            """.trimIndent()
        )
        val config = readYAMLConfig<AdderConfiguration>(Path.of(temporaryFile.absolutePath))
        val plugin = AdderPlugin(config)

        plugin.launchUpdateConfigFileChanged(temporaryFile.absolutePath)
        Thread.sleep(1000)

        Files.writeString(
            temporaryFile.toPath(), """
                pipeline:
                    manager-host: localhost:39181
                    plugin-id: rate
                add:
                    - key: version
                      value: 2
            """.trimIndent()
        )
        Thread.sleep(1000)

        assertEquals("2", plugin.config.add.get(0).value)
    }

    @Test
    fun `change of different file should not update config`() {
        val temporaryFile = File.createTempFile("hyperion-add-config", "yaml")
        Files.writeString(
            temporaryFile.toPath(), """
                pipeline:
                    manager-host: localhost:39181
                    plugin-id: rate
                add:
                    - key: version
                      value: 1
            """.trimIndent()
        )
        val config = readYAMLConfig<AdderConfiguration>(Path.of(temporaryFile.absolutePath))
        val plugin = AdderPlugin(config)

        plugin.launchUpdateConfigFileChanged(temporaryFile.absolutePath)
        Thread.sleep(1000)

        val temporaryFile2 = File.createTempFile("hyperion-add-config", "yaml")
        Files.writeString(
            temporaryFile2.toPath(), """
                pipeline:
                    manager-host: localhost:39181
                    plugin-id: rate
                add:
                    - key: version
                      value: 2
            """.trimIndent()
        )
        Thread.sleep(1000)

        assertEquals("1", plugin.config.add.get(0).value)
    }
}
