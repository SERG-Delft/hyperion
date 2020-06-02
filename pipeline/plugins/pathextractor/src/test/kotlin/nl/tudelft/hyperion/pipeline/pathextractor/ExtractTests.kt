package nl.tudelft.hyperion.pipeline.pathextractor

import com.fasterxml.jackson.databind.ObjectMapper
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ExtractTests {
    @Test
    fun testRenameLogLine() {
        val config = Configuration(
            "log4j_file",
            "src/main/java",
            ".java",
            PipelinePluginConfiguration("pathExtractor", "1.2.3.4:4567")
        )

        val input = """{ "log4j_file" :  "com.sap.enterprises.server.impl.TransportationService" }"""
        val expected = """{"log4j_file":"src/main/java/com/sap/enterprises/server/impl/TransportationService.java"}"""

        val mapper = ObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extractPath(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testParentNull() {
        val config = Configuration(
            "nonExisting",
            "src/main/java",
            ".java",
            PipelinePluginConfiguration("pathExtractor", "1.2.3.4:4567")
        )

        val input = """{ "log4j_file" :  "com.sap.enterprises.server.impl.TransportationService" }"""

        val mapper = ObjectMapper()

        val treeExpected = mapper.readTree(input)
        val treeActual = mapper.readTree(extractPath(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testFieldNotString() {
        val config = Configuration(
            "log4j_file",
            "src/main/java",
            ".java",
            PipelinePluginConfiguration("pathExtractor", "1.2.3.4:4567")
        )

        val input = """{"log4j_file":true}"""

        val expected = input
        val actual = extractPath(input, config)

        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun testInputNotJSONObject() {
        val config = Configuration(
            "log4j_file",
            "src/main/java",
            ".java",
            PipelinePluginConfiguration("pathExtractor", "1.2.3.4:4567")
        )

        val input = """true"""

        val expected = input
        val actual = extractPath(input, config)

        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun testKotlinNamingSupport() {
        val config = Configuration(
            "log4j_file",
            "src/main/kotlin",
            ".kt",
            PipelinePluginConfiguration("pathExtractor", "1.2.3.4:4567")
        )

        val input = """{ "log4j_file" :  "com.sap.enterprises.server.impl.TransportationServiceKt" }"""
        val expected = """{"log4j_file":"src/main/kotlin/com/sap/enterprises/server/impl/TransportationService.kt"}"""

        val mapper = ObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extractPath(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }
}
