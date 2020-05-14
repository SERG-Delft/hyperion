package nl.tudelft.hyperion.aggregator.api

import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import nl.tudelft.hyperion.aggregator.utils.TestWithoutLogging
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class LogEntryTest : TestWithoutLogging() {
    @Test
    fun `LogEntry should be able to parse from a string`() {
        val content = """
            {
                "project": "TestProject",
                "version": "v1.0.0",
                "severity": "INFO",
                "location": {
                    "file": "com.test.file",
                    "line": 10
                },
                "timestamp": "2020-05-07T11:22:00.644Z"
            }
        """.trimIndent()

        val entry = LogEntry.parse(content)
        assertEquals(
            entry,
            LogEntry(
                "TestProject",
                "v1.0.0",
                "INFO",
                LogLocation(
                    "com.test.file",
                    10
                ),
                DateTime.parse("2020-05-07T11:22:00.644Z")
            )
        )
    }

    @Test
    fun `LogEntry should coerce types to a reasonable degree`() {
        // Test coerce line number from string to int.
        val content = """
            {
                "project": "TestProject",
                "version": "v1.0.0",
                "severity": "INFO",
                "location": {
                    "file": "com.test.file",
                    "line": "10"
                },
                "timestamp": "2020-05-07T11:22:00.644Z"
            }
        """.trimIndent()

        val entry = LogEntry.parse(content)
        assertEquals(
            entry,
            LogEntry(
                "TestProject",
                "v1.0.0",
                "INFO",
                LogLocation(
                    "com.test.file",
                    10
                ),
                DateTime.parse("2020-05-07T11:22:00.644Z")
            )
        )
    }

    @TestFactory
    fun `LogEntry should support alternative date formats`() = listOf(
        """"2020-05-07T11:22:00.644Z"""",
        """1588850520644""",
        """"2020-05-07T11:22:00.644"""",
        """"2020-05-07T11:22:00.644+00""""
    ).map {
        DynamicTest.dynamicTest("date format $it should be accepted") {
            val content = """
                {
                    "project": "TestProject",
                    "version": "v1.0.0",
                    "severity": "INFO",
                    "location": {
                        "file": "com.test.file",
                        "line": 10
                    },
                    "timestamp": $it
                }
            """.trimIndent()

            val entry = LogEntry.parse(content)
            assertEquals(
                entry,
                LogEntry(
                    "TestProject",
                    "v1.0.0",
                    "INFO",
                    LogLocation(
                        "com.test.file",
                        10
                    ),
                    DateTime.parse("2020-05-07T11:22:00.644Z")
                )
            )
        }
    }

    @Test
    fun `LogEntry should not error on extra properties`() {
        val content = """
            {
                "project": "TestProject",
                "version": "v1.0.0",
                "severity": "INFO",
                "some": {
                    "extra": ["property"]
                },
                "location": {
                    "file": "com.test.file",
                    "line": 10
                },
                "timestamp": "2020-05-07T11:22:00.644Z"
            }
        """.trimIndent()

        assertDoesNotThrow("Unrecognized field \"some\"") {
            LogEntry.parse(content)
        }
    }

    @Test
    fun `LogEntry should throw on missing properties`() {
        val content = """
            {
                "version": "v1.0.0",
                "severity": "INFO",
                "location": {
                    "file": "com.test.file",
                    "line": 10
                },
                "timestamp": "2020-05-07T11:22:00.644Z"
            }
        """.trimIndent()

        assertThrows<MissingKotlinParameterException> {
            LogEntry.parse(content)
        }
    }
}
