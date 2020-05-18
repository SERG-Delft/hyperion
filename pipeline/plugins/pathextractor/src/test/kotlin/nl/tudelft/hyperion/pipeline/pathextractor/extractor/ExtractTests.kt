package nl.tudelft.hyperion.pipeline.pathextractor.extractor

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import nl.tudelft.hyperion.pipeline.pathextractor.Configuration
import nl.tudelft.hyperion.pipeline.pathextractor.extractPath
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ExtractTests {
    @Test
    fun testRenameLogLine() {
        val config = Configuration(
                "log4j_file",
                "src/main/java",
                PipelinePluginConfiguration("pathExtractor", "1.2.3.4:4567")
        )

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
                "nonExisting",
                "src/main/java",
                PipelinePluginConfiguration("pathExtractor", "1.2.3.4:4567")
        )

        val input = "{ \"log4j_file\" :  \"com.sap.enterprises.server.impl.TransportationService\" }"

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(input)
        val treeActual = mapper.readTree(extractPath(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }
}
