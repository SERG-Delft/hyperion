package nl.tudelft.hyperion.extractor

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import nl.tudelft.hyperion.pluginmanager.RedisConfig
import org.junit.jupiter.api.Assertions

class MainTest() {
    @Test
    fun testRenameLogLine() {
        val config = Configuration("message", "\\[.+?\\] INFO [^:]+:(\\d+) (-) .+", listOf(Extract("location.line", "number"), Extract("dash", "string")), RedisConfig("host", 6379), null, "plugin")

        val expected = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:10 - Test\", \"location\" : {\"line\" : 10}, \"dash\" : \"-\"}"

        val input = "{\"message\":\"[Mar 20 11:11:11] INFO some/file/name:10 - Test\"}"

        val mapper = jacksonObjectMapper()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }
}