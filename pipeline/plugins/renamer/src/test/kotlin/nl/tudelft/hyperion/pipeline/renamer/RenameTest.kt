package nl.tudelft.hyperion.pipeline.renamer

import com.fasterxml.jackson.databind.ObjectMapper
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class RenameTest {
    @Test
    fun testRenameLogLine() {
        val config = Configuration(
            listOf(
                Rename(
                    "log_line",
                    "location.line"
                )
            ),
            PipelinePluginConfiguration("renamer", "1.2.3.4:4567")
        )

        val expected = """{
          "project" : "some unique identifier for project, such as the repo name or package",
          "version" : "some way to represent the version, such as a git tag or hash",
          "severity" : "some severity, no fixed format",
          "timestamp" : "ISO 8601 timestamp format",
          "location" : { "line": 10 }
        }""".trimIndent()

        val input = """{
          "project" : "some unique identifier for project, such as the repo name or package",
          "version" : "some way to represent the version, such as a git tag or hash",
          "severity" : "some severity, no fixed format",
          "timestamp" : "ISO 8601 timestamp format",
          "log_line" : 10
        }""".trimIndent()

        val mapper = ObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(rename(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testRenameLogLineString() {
        val config = Configuration(
            listOf(
                Rename(
                    "log_line",
                    "location.line"
                )
            ),
            PipelinePluginConfiguration("renamer", "1.2.3.4:4567")
        )

        val expected = """{
          "project" : "some unique identifier for project, such as the repo name or package",
          "version" : "some way to represent the version, such as a git tag or hash",
          "severity" : "some severity, no fixed format",
          "timestamp" : "ISO 8601 timestamp format",
          "location" : { "line": "10" }
        }""".trimIndent()

        val input = """{
          "project" : "some unique identifier for project, such as the repo name or package",
          "version" : "some way to represent the version, such as a git tag or hash",
          "severity" : "some severity, no fixed format",
          "timestamp" : "ISO 8601 timestamp format",
          "log_line" : "10"
        }""".trimIndent()

        val mapper = ObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(rename(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testRenameNotFound() {
        val config = Configuration(
            listOf(
                Rename(
                    "log_line",
                    "location.line"
                )
            ),
            PipelinePluginConfiguration("renamer", "1.2.3.4:4567")
        )

        val input = """{
          "project" : "some unique identifier for project, such as the repo name or package",
          "version" : "some way to represent the version, such as a git tag or hash",
          "severity" : "some severity, no fixed format",
          "timestamp" : "ISO 8601 timestamp format",
          "log-line" : 10
        }""".trimIndent()

        val mapper = ObjectMapper()

        val treeBefore = mapper.readTree(input)
        val treeAfter = mapper.readTree(rename(input, config))

        Assertions.assertEquals(treeBefore, treeAfter)
    }

    @Test
    fun testRenameNotFoundContinue() {
        val config = Configuration(
            listOf(
                Rename(
                    "log_line",
                    "location.line"
                ),
                Rename(
                    "log-line",
                    "location"
                )
            ),
            PipelinePluginConfiguration("renamer", "1.2.3.4:4567")
        )

        val input = """{
          "project" : "some unique identifier for project, such as the repo name or package",
          "version" : "some way to represent the version, such as a git tag or hash",
          "severity" : "some severity, no fixed format",
          "timestamp" : "ISO 8601 timestamp format",
          "log-line" : 10
        }""".trimIndent()

        val expected = """{
          "project" : "some unique identifier for project, such as the repo name or package",
          "version" : "some way to represent the version, such as a git tag or hash",
          "severity" : "some severity, no fixed format",
          "timestamp" : "ISO 8601 timestamp format",
          "location" : 10
        }""".trimIndent()

        val mapper = ObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(rename(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testInputNotJSON() {
        val config = Configuration(
            listOf(
                Rename(
                    "log_line",
                    "location.line"
                )
            ),
            PipelinePluginConfiguration("renamer", "1.2.3.4:4567")
        )

        val input = "true"
        val output = rename(input, config)

        Assertions.assertEquals(input, output)
    }

    @Test
    fun testRenameTargetExistsAndIsNotObject() {
        val config = Configuration(
            listOf(
                Rename(
                    "log_line",
                    "location.line"
                )
            ),
            PipelinePluginConfiguration("renamer", "1.2.3.4:4567")
        )

        val input = """{
          "location": "not an object",
          "log_line": 10
        }""".trimIndent()

        val mapper = ObjectMapper()

        val treeBefore = mapper.readTree(input)
        val treeAfter = mapper.readTree(rename(input, config))

        Assertions.assertEquals(treeBefore, treeAfter)
    }

    @Test
    fun testRenameNestedField() {
        val config = Configuration(
            listOf(
                Rename(
                    "location.line",
                    "log_line"
                )
            ),
            PipelinePluginConfiguration("renamer", "1.2.3.4:4567")
        )

        val input = """{
          "location": { "line" : 10}
        }""".trimIndent()

        val expected = """{
          "location": {},
          "log_line" : 10
        }""".trimIndent()

        val mapper = ObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(rename(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testNestedFieldToNestedField() {
        val config = Configuration(
            listOf(
                Rename(
                    "location.line",
                    "log.line"
                )
            ),
            PipelinePluginConfiguration("renamer", "1.2.3.4:4567")
        )

        val input = """{
          "location": { "line" : 10}
        }""".trimIndent()

        val expected = """{
          "location": {},
          "log" : { "line" : 10}
        }""".trimIndent()

        val mapper = ObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(rename(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `If nested field does not exist, nothing should be done`() {
        val config = Configuration(
            listOf(
                Rename(
                    "log.line",
                    "log_line"
                )
            ),
            PipelinePluginConfiguration("renamer", "1.2.3.4:4567")
        )

        val input = """{
          "location": 10
        }""".trimIndent()

        val mapper = ObjectMapper()

        val treeExpected = mapper.readTree(input)
        val treeActual = mapper.readTree(rename(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `Test target path is nested existing path`() {
        val config = Configuration(
            listOf(
                Rename(
                    "log_line",
                    "location.line"
                )
            ),
            PipelinePluginConfiguration("renamer", "1.2.3.4:4567")
        )

        val input = """{
          "log_line": 10,
          "location" : { "line" : "test"}
        }""".trimIndent()

        val expected = """{
          "location" : { "line" : 10 }
        }""".trimIndent()

        val mapper = ObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(rename(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }
}
