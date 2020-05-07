package nl.tudelft.hyperion.renamer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.file.Path

class MainTest() {
    @Test
    fun testRenameLogLine() {
        val config = Configuration(RedisConfig("localhost", 3800, 1, 3800, 2), listOf(Rename("log_line", "location.line")))

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
        val config = Configuration(RedisConfig("localhost", 3800,1, 3800, 2), listOf(Rename("log_line", "location.line")))

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

    @Test
    fun testConfigFromFile() {
        val config = Configuration.load(Path.of("./config.yaml").toAbsolutePath())

        Assertions.assertTrue(config.redis != null)
        Assertions.assertTrue(config.rename != null)
    }

    @Test
    fun redisAutoConfig() {
        val config = Configuration(RedisConfig("localhost", null,1, null, 2), listOf(Rename("log_line", "location.line")))

        Assertions.assertTrue(config.redis.portIn == 6380)
        Assertions.assertTrue(config.redis.portOut == 6381)
    }
}