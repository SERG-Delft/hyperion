package nl.tudelft.hyperion.pipeline.plugins.adder

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions

class AdderTest {
    @Test
    fun `add one parent level field`() {
        val config = listOf(
            AddConfiguration("version", "1.0.2")
        )

        val input = """{"message":"hey"}"""
        val expected = """{"message":"hey","version":"1.0.2"}"""

        Assertions.assertEquals(expected, adder(input, config))
    }

    @Test
    fun `add two parent level fields`() {
        val config = listOf(
            AddConfiguration("version", "1.0.2"),
            AddConfiguration("secret", "egg")
        )

        val input = """{"message":"hey"}"""
        val expected = """{"message":"hey","version":"1.0.2","secret":"egg"}"""

        Assertions.assertEquals(expected, adder(input, config))
    }

    @Test
    fun `add two same key parent level fields shouldn't overwrite`() {
        val config = listOf(
            AddConfiguration("version", "1.0.2"),
            AddConfiguration("version", "2.1")
        )

        val input = """{"message":"hey"}"""
        val expected = """{"message":"hey","version":"1.0.2"}"""

        Assertions.assertEquals(expected, adder(input, config))
    }

    @Test
    fun `add new field 1 level deep`() {
        val config = listOf(
            AddConfiguration("code.version", "1.0.2")
        )

        val input = """{"message":"hey"}"""
        val expected = """{"message":"hey","code":{"version":"1.0.2"}}""".trimMargin()

        Assertions.assertEquals(expected, adder(input, config))
    }

    @Test
    fun `add field to existing 1 level deep`() {
        val config = listOf(
            AddConfiguration("code.version", "1.0.2")
        )

        val input = """{"code":{"author":"superman"}}"""
        val expected = """{"code":{"author":"superman","version":"1.0.2"}}"""

        Assertions.assertEquals(expected, adder(input, config))
    }

    @Test
    fun `add same field to existing 1 level deep shouldn't overwrite`() {
        val config = listOf(
            AddConfiguration("code.author", "batman")
        )

        val input = """{"code":{"author":"superman"}}"""
        val expected = """{"code":{"author":"superman"}}"""

        Assertions.assertEquals(expected, adder(input, config))
    }
}