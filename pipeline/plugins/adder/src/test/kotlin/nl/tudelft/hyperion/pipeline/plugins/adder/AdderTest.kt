package nl.tudelft.hyperion.pipeline.plugins.adder

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions

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
}