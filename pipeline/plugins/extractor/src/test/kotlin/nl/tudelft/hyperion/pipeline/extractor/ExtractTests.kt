package nl.tudelft.hyperion.pipeline.extractor

import com.fasterxml.jackson.databind.ObjectMapper
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ExtractTests {
    private val mapper = ObjectMapper()

    @Test
    fun testSimpleMessage() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message",
                    "\\[.+?\\] INFO [^:]+:(\\d+) (-) .+",
                    listOf(Extract("location.line", Type.NUMBER), Extract("dash", Type.STRING))
                )
            )
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:10 - Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:10 - Test",
            | "location" : {"line" : 10}, "dash" : "-"}""".trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testMessageWithNumber() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message",
                    "\\[.+?\\] INFO [^:]+:(\\d+) (3) .+",
                    listOf(Extract("location.line", Type.NUMBER), Extract("dash", Type.NUMBER))
                )
            )
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:10 3 Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:10 3 Test",
            | "location" : {"line" : 10}, "dash" : 3}""".trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testMessagePathExists() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message",
                    "\\[.+?\\] INFO [^:]+:(\\d+) (-) .+",
                    listOf(Extract("location.line", Type.NUMBER), Extract("dash", Type.STRING))
                )
            )
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:10 - Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:10 - Test",
            | "location" : {"line" : 10}, "dash" : "-"}""".trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testLastPathPartIsDoubleType() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message",
                    "\\[.+?\\] INFO [^:]+:(2.5) .+",
                    listOf(Extract("location.line", Type.DOUBLE))
                )
            )
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:2.5 Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:2.5 Test",
            | "location" : {"line" : 2.5}}""".trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testNonHierarchicalTargetPath() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message",
                    "\\[.+?\\] INFO [^:]+:(-) .+",
                    listOf(Extract("location", Type.STRING))
                )
            )
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:- Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:- Test",
            | "location" : "-"}""".trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testNumberType() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message",
                    "\\[.+?\\] INFO [^:]+:(\\d+) .+",
                    listOf(Extract("location", Type.NUMBER))
                )
            )
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:1934 Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:1934 Test",
            | "location" : 1934}""".trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testDoubleType() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message",
                    "\\[.+?\\] INFO [^:]+:(\\d+.\\d+) .+",
                    listOf(Extract("location", Type.DOUBLE))
                )
            )
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:34.567 Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:34.567 Test",
            | "location" : 34.567}""".trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testStringType() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message",
                    "\\[.+?\\] INFO [^:]+:(\\d+.\\d+) .+",
                    listOf(Extract("location.line", Type.STRING))
                )
            )
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:34.567 Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:34.567 Test",
            | "location" : { "line" : "34.567"}}""".trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testDeepHierarchy() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message",
                    "\\[.+?\\] INFO [^:]+:(\\d+) - .+",
                    listOf(Extract("location.line.numeric", Type.NUMBER))
                )
            )
        )

        val input = """{"message":"[Mar 20 11:11:11] INFO some/file/name:34 - Test"}"""
        val expected = """{"message":"[Mar 20 11:11:11] INFO some/file/name:34 - Test",
            | "location" : {"line" : { "numeric" : 34}}}""".trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `Test if multiple fields can be extracted`() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"),
            listOf(
                ExtractableFieldConfiguration(
                    "message",
                    "\\[.+?\\] INFO [^:]+:(\\d+) - .+",
                    listOf(Extract("location.line", Type.NUMBER))
                ),
                ExtractableFieldConfiguration(
                    "message_2",
                    "\\[.+?\\] INFO [^:]+:(\\d+) - .+",
                    listOf(Extract("location.line_2", Type.NUMBER))
                )
            )
        )

        val input = """
                {
                    "message":"[Mar 20 11:11:11] INFO some/file/name:34 - Test", 
                    "message_2":"[Mar 20 11:11:11] INFO some/file/name:35 - Test"
                }
        """.trimMargin()

        val expected = """
                {
                    "message":"[Mar 20 11:11:11] INFO some/file/name:34 - Test", 
                    "message_2":"[Mar 20 11:11:11] INFO some/file/name:35 - Test",
                    "location" : 
                        {
                            "line" : 34,
                            "line_2" : 35
                        }
                }
        """.trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `Multiple fields should overwrite one another in order`() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"),
            listOf(
                ExtractableFieldConfiguration(
                    "message",
                    "\\[.+?\\] INFO [^:]+:(\\d+) - .+",
                    listOf(Extract("location.line", Type.NUMBER))
                ),
                ExtractableFieldConfiguration(
                    "message_2",
                    "\\[.+?\\] INFO [^:]+:(\\d+) - .+",
                    listOf(Extract("location.line", Type.NUMBER))
                )
            )
        )

        val input = """
                {
                    "message":"[Mar 20 11:11:11] INFO some/file/name:34 - Test", 
                    "message_2":"[Mar 20 11:11:11] INFO some/file/name:35 - Test"
                }
        """.trimMargin()

        val expected = """
                {
                    "message":"[Mar 20 11:11:11] INFO some/file/name:34 - Test", 
                    "message_2":"[Mar 20 11:11:11] INFO some/file/name:35 - Test",
                    "location" : 
                        {
                            "line" : 35
                        }
                }
        """.trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `If there are additional unmatched extraction patterns, they should not be considered`() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message",
                    "(1) (2) (3)",
                    listOf(
                        Extract("numeric.1", Type.NUMBER),
                        Extract("numeric.2", Type.NUMBER),
                        Extract("numeric.3", Type.NUMBER),
                        Extract("numeric.4", Type.NUMBER)
                    )
                )
            )
        )

        val input = """{"message":"1 2 3 4"}"""
        val expected = """{"message":"1 2 3 4",
            | "numeric" : {"1" : 1, "2" : 2, "3": 3}}""".trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `If there are capture groups without an extraction pattern, they should not be matched`() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message",
                    "(1) (2) (3) (4) (5)",
                    listOf(
                        Extract("numeric.1", Type.NUMBER),
                        Extract("numeric.2", Type.NUMBER),
                        Extract("numeric.3", Type.NUMBER)
                    )
                )
            )
        )

        val input = """{"message":"1 2 3 4 5"}"""
        val expected = """{"message":"1 2 3 4 5",
            | "numeric" : {"1" : 1, "2" : 2, "3": 3}}""".trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `If field value does not exist, it should not be considered`() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message",
                    "(1)",
                    listOf(
                        Extract("numeric.1", Type.NUMBER)
                    )
                )
            )
        )

        val input = """{"nonExistent":"1 2 3 4 5"}"""

        val treeExpected = mapper.readTree(input)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `on invalid message return input`() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message",
                    "(1)",
                    listOf(
                        Extract("numeric.1", Type.NUMBER)
                    )
                )
            )
        )

        val input = """chicken"""

        val ret = extract(input, config)

        Assertions.assertEquals(input, ret)
    }

    @Test
    fun `nested extract field`() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
            ExtractableFieldConfiguration(
                "message.line",
                "(1)",
                listOf(
                    Extract("numeric.1", Type.NUMBER)
                    )
                )
            )
        )

        val input = """{ "message" : { "line" : "1"} }"""
        val expected = """{ "message" : { "line" : "1"}, "numeric" : { "1" : 1}}""".trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `int type while not being int type should add as string, nested`() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message.line",
                    "(n)",
                    listOf(
                        Extract("numeric.1", Type.NUMBER)
                    )
                )
            )
        )

        val input = """{ "message" : { "line" : "n"} }"""
        val expected = """{ "message" : { "line" : "n"}, "numeric" : { "1" : "n"}}""".trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `double type while not being double type should add as string`() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message.line",
                    "(n)",
                    listOf(
                        Extract("numeric.1", Type.DOUBLE)
                    )
                )
            )
        )

        val input = """{ "message" : { "line" : "n"} }"""
        val expected = """{ "message" : { "line" : "n"}, "numeric" : { "1" : "n"}}""".trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `nothing should happen when field does not exist`() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "messag",
                    "(n)",
                    listOf(
                        Extract("numeric.1", Type.NUMBER)
                    )
                )
            )
        )

        val input = """{ "message" : "message" }"""

        val treeExpected = mapper.readTree(input)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `nothing should happen when field does not exist, nested`() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message.lin",
                    "(n)",
                    listOf(
                        Extract("numeric.1", Type.NUMBER)
                    )
                )
            )
        )

        val input = """{ "message" : { "line" : "1"} }"""

        val treeExpected = mapper.readTree(input)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `nothing should happen when path does not exist`() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "messag.lin",
                    "(n)",
                    listOf(
                        Extract("numeric.1", Type.NUMBER)
                    )
                )
            )
        )

        val input = """{ "message" : { "line" : "1"} }"""

        val treeExpected = mapper.readTree(input)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `when target field ends at a leaf that was configured to be a node, nothing should be extracted`() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message.line",
                    "(1)",
                    listOf(
                        Extract("numeric.1", Type.NUMBER)
                    )
                )
            )
        )

        val input = """{ "message" : { "line" : "1"}, "numeric" : "not an object" }"""

        val treeExpected = mapper.readTree(input)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `int type while not being int type should add as string`() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message.line",
                    "(n)",
                    listOf(
                        Extract("numeric", Type.NUMBER)
                    )
                )
            )
        )

        val input = """{ "message" : { "line" : "n"} }"""
        val expected = """{ "message" : { "line" : "n"}, "numeric" : "n" }""".trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `when target field ends at a leaf that was configured to be a node, nothing should be extracted, double`() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message.line",
                    "(1.5)",
                    listOf(
                        Extract("numeric.1", Type.DOUBLE)
                    )
                )
            )
        )

        val input = """{ "message" : { "line" : "1.5"}, "numeric" : "not an object" }"""

        val treeExpected = mapper.readTree(input)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `when target field ends at a leaf that was configured to be a node, nothing should be extracted, string`() {
        val config = Configuration(
            PipelinePluginConfiguration("extractor", "1.2.3.4:4567"), listOf(
                ExtractableFieldConfiguration(
                    "message.line",
                    "(string)",
                    listOf(
                        Extract("numeric.1", Type.STRING)
                    )
                )
            )
        )

        val input = """{ "message" : { "line" : "string"}, "numeric" : "not an object" }"""

        val treeExpected = mapper.readTree(input)
        val treeActual = mapper.readTree(extract(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }
}
