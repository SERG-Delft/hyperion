package nl.tudelft.hyperion.renamer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Path
import nl.tudelft.hyperion.pluginmanager.RedisConfig

class MainTest() {
    @Test
    fun testRenameLogLine() {
        val config = Configuration(listOf(Rename("log_line", "location.line")), RedisConfig("host", 6379), null,"extractor")

        val expected = "{\n" +
                "  \"project\" : \"some unique identifier for project, such as the repo name or package\",\n" +
                "  \"version\" : \"some way to represent the version, such as a git tag or hash\",\n" +
                "  \"severity\" : \"some severity, no fixed format\",\n" +
                "  \"timestamp\" : \"ISO 8601 timestamp format\",\n" +
                "  \"location.line\" : 10\n" +
                "}"

        val input = "{\n" +
                "  \"project\" : \"some unique identifier for project, such as the repo name or package\",\n" +
                "  \"version\" : \"some way to represent the version, such as a git tag or hash\",\n" +
                "  \"severity\" : \"some severity, no fixed format\",\n" +
                "  \"timestamp\" : \"ISO 8601 timestamp format\",\n" +
                "  \"log_line\" : 10\n" +
                "}"

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(rename(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testRenameNotFound() {
        val config = Configuration(listOf(Rename("log_line", "location.line")), RedisConfig("host", 6379), null,"extractor")

        val input = "{\n" +
                "  \"project\" : \"some unique identifier for project, such as the repo name or package\",\n" +
                "  \"version\" : \"some way to represent the version, such as a git tag or hash\",\n" +
                "  \"severity\" : \"some severity, no fixed format\",\n" +
                "  \"timestamp\" : \"ISO 8601 timestamp format\",\n" +
                "  \"log-line\" : 10\n" +
                "}"

        val mapper = jacksonObjectMapper()

        val treeBefore = mapper.readTree(input)
        val treeAfter = mapper.readTree(rename(input, config))

        Assertions.assertEquals(treeBefore, treeAfter)
    }
}