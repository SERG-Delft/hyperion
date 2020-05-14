package nl.tudelft.hyperion.pathextractor.extractor

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import nl.tudelft.hyperion.pathextractor.Configuration
import nl.tudelft.hyperion.pathextractor.extractPath
import nl.tudelft.hyperion.pipeline.PipelineRedisConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ExtractTests {
    @Test
    fun testExtract() {
        val config = Configuration(
                PipelineRedisConfiguration("host", 6379),
                null,
                "plugin",
                "log4j_file",
                "src/main/java")

        val input = "{ \"log4j_file\" :  \"com.sap.enterprises.server.impl.TransportationService\" }"
        val expected = "{\"log4j_file\":\"src/main/java/com/sap/enterprises/server/impl/TransportationService.java\"}"

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extractPath(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testParentNull() {
        val config = Configuration(
                PipelineRedisConfiguration("host", 6379),
                null,
                "plugin",
                "log4j_nonExisting",
                "src/main/java")

        val input = "{ \"log4j_file\" :  \"com.sap.enterprises.server.impl.TransportationService\" }"

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(input)
        val treeActual = mapper.readTree(extractPath(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }
}
