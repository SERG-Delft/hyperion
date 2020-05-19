package nl.tudelft.hyperion.pipeline.extractor.extract

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import nl.tudelft.hyperion.pipeline.extractor.Configuration
import nl.tudelft.hyperion.pipeline.extractor.Extract
import nl.tudelft.hyperion.pipeline.extractor.extract
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions

class ExtractTests {
    @Test
    fun testSimpleMessage() {
        val config = Configuration(
                PipelinePluginConfiguration("extractor", "1.2.3.4:4567"),
                "message",
                "\\[.+?\\] INFO [^:]+:(\\d+) (-) .+",
                listOf(Extract("location.line", "number"), Extract("dash", "string"))
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:10 - Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:10 - Test",
            | "location" : {"line" : 10}, "dash" : "-"}""".trimMargin()

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testMessageWithNumber() {
        val config = Configuration(
                PipelinePluginConfiguration("extractor", "1.2.3.4:4567"),
                "message",
                "\\[.+?\\] INFO [^:]+:(\\d+) (3) .+",
                listOf(Extract("location.line", "number"), Extract("dash", "number"))
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:10 3 Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:10 3 Test",
            | "location" : {"line" : 10}, "dash" : 3}""".trimMargin()

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testMessagePathExists() {
        val config = Configuration(
                PipelinePluginConfiguration("extractor", "1.2.3.4:4567"),
                "message",
                "\\[.+?\\] INFO [^:]+:(\\d+) (-) .+",
                listOf(Extract("location.line", "number"), Extract("dash", "somethingElse"))
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:10 - Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:10 - Test",
            | "location" : {"line" : 10}, "dash" : "-"}""".trimMargin()

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testLastPathPartIsDefaultType() {
        val config = Configuration(
                PipelinePluginConfiguration("extractor", "1.2.3.4:4567"),
                "message",
                "\\[.+?\\] INFO [^:]+:(-) .+",
                listOf(Extract("location.line", "somethingElse"))
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:- Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:- Test",
            | "location" : {"line" : "-"}}""".trimMargin()

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testLastPathPartIsDoubleType() {
        val config = Configuration(
                PipelinePluginConfiguration("extractor", "1.2.3.4:4567"),
                "message",
                "\\[.+?\\] INFO [^:]+:(2.5) .+",
                listOf(Extract("location.line", "double"))
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:2.5 Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:2.5 Test",
            | "location" : {"line" : 2.5}}""".trimMargin()

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testNonHierarchicalTargetPath() {
        val config = Configuration(
                PipelinePluginConfiguration("extractor", "1.2.3.4:4567"),
                "message",
                "\\[.+?\\] INFO [^:]+:(-) .+",
                listOf(Extract("location", "string"))
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:- Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:- Test",
            | "location" : "-"}""".trimMargin()

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testNumberType() {
        val config = Configuration(
                PipelinePluginConfiguration("extractor", "1.2.3.4:4567"),
                "message",
                "\\[.+?\\] INFO [^:]+:(\\d+) .+",
                listOf(Extract("location", "number"))
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:1934 Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:1934 Test",
            | "location" : 1934}""".trimMargin()

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testDoubleType() {
        val config = Configuration(
                PipelinePluginConfiguration("extractor", "1.2.3.4:4567"),
                "message",
                "\\[.+?\\] INFO [^:]+:(\\d+.\\d+) .+",
                listOf(Extract("location", "double"))
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:34.567 Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:34.567 Test",
            | "location" : 34.567}""".trimMargin()

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testStringType() {
        val config = Configuration(
                PipelinePluginConfiguration("extractor", "1.2.3.4:4567"),
                "message",
                "\\[.+?\\] INFO [^:]+:(\\d+.\\d+) .+",
                listOf(Extract("location.line", "string"))
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:34.567 Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:34.567 Test",
            | "location" : { "line" : "34.567"}}""".trimMargin()

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testDeepHierarchy() {
        val config = Configuration(
                PipelinePluginConfiguration("extractor", "1.2.3.4:4567"),
                "message",
                "\\[.+?\\] INFO [^:]+:(\\d+) - .+",
                listOf(Extract("location.line.numeric", "number"))
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:34 - Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:34 - Test",
            | "location" : {"line" : { "numeric" : 34}}}""".trimMargin()

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }
}
