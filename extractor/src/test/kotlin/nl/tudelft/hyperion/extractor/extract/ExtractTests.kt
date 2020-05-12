package nl.tudelft.hyperion.extractor.extract

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import nl.tudelft.hyperion.extractor.Configuration
import nl.tudelft.hyperion.extractor.Extract
import nl.tudelft.hyperion.extractor.extract
import org.junit.jupiter.api.Test
import nl.tudelft.hyperion.pluginmanager.RedisConfig
import org.junit.jupiter.api.Assertions

class ExtractTests() {
    @Test
    fun testSimpleMessage() {
        val config = Configuration("message", "\\[.+?\\] INFO [^:]+:(\\d+) (-) .+", listOf(Extract("location.line", "number"), Extract("dash", "string")), RedisConfig("host", 6379), null, "plugin")

        val input = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:10 - Test\"}"
        val expected = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:10 - Test\", \"location\" : {\"line\" : 10}, \"dash\" : \"-\"}"

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testMessageWithNumber() {
        val config = Configuration("message", "\\[.+?\\] INFO [^:]+:(\\d+) (3) .+", listOf(Extract("location.line", "number"), Extract("dash", "number")), RedisConfig("host", 6379), null, "plugin")

        val input = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:10 3 Test\"}"
        val expected = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:10 3 Test\", \"location\" : {\"line\" : 10}, \"dash\" : 3}"

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testMessagePathExists() {
        val config = Configuration("message", "\\[.+?\\] INFO [^:]+:(\\d+) (-) .+", listOf(Extract("location.line", "number"), Extract("dash", "somethingElse")), RedisConfig("host", 6379), null, "plugin")

        val input = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:10 - Test\", \"location\" : {\"document\" : \"location.kt\"}}"
        val expected = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:10 - Test\", \"location\": {\"document\" : \"location.kt\", \"line\" : 10}, \"dash\" : \"-\"}"

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testLastPathPartIsDefaultType() {
        val config = Configuration("message", "\\[.+?\\] INFO [^:]+:(-) .+", listOf(Extract("location.line", "somethingElse")), RedisConfig("host", 6379), null, "plugin")

        val input = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:- Test\"}"
        val expected = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:- Test\", \"location\": {\"line\" : \"-\"}}"

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testLastPathPartIsDoubleType() {
        val config = Configuration("message", "\\[.+?\\] INFO [^:]+:(2.5) .+", listOf(Extract("location.line", "double")), RedisConfig("host", 6379), null, "plugin")

        val input = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:2.5 Test\"}"
        val expected = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:2.5 Test\", \"location\": {\"line\" : 2.5}}"

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testNonHierarchicalTargetPath() {
        val config = Configuration("message", "\\[.+?\\] INFO [^:]+:(-) .+", listOf(Extract("location", "string")), RedisConfig("host", 6379), null, "plugin")

        val input = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:- Test\"}"
        val expected = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:- Test\", \"location\": \"-\"}"

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testNumberType() {
        val config = Configuration("message", "\\[.+?\\] INFO [^:]+:(\\d+) .+", listOf(Extract("location", "number")), RedisConfig("host", 6379), null, "plugin")

        val input = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:1934 Test\"}"
        val expected = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:1934 Test\", \"location\": 1934}"

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testDoubleType() {
        val config = Configuration("message", "\\[.+?\\] INFO [^:]+:(\\d+.\\d+) .+", listOf(Extract("location", "double")), RedisConfig("host", 6379), null, "plugin")

        val input = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:34.567 Test\"}"
        val expected = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:34.567 Test\", \"location\": 34.567}"

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testDeepHierarchy() {
        val config = Configuration("message", "\\[.+?\\] INFO [^:]+:(\\d+) - .+", listOf(Extract("location.line.numeric", "number")), RedisConfig("host", 6379), null, "plugin")

        val input = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:34 - Test\"}"
        val expected = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:34 - Test\", \"location\": {\"line\" : { \"numeric\" :  34}}}"

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }
}