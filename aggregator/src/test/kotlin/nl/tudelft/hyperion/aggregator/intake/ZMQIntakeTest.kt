package nl.tudelft.hyperion.aggregator.intake

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.aggregator.Configuration
import nl.tudelft.hyperion.aggregator.utils.TestWithoutLogging
import nl.tudelft.hyperion.aggregator.workers.AggregationManager
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import org.joda.time.DateTime
import org.junit.jupiter.api.Test

class ZMQIntakeTest : TestWithoutLogging() {
    @Test
    fun `Received messages will be aggregated if valid`() {
        val aggregateMock = mockk<AggregationManager>(relaxed = true)

        val intake = ZMQIntake(
            Configuration("", 0, 1, 1),
            aggregateMock,
            PipelinePluginConfiguration("Aggregator", "localhost:12346")
        )

        runBlocking {
            intake.onMessageReceived(
                """
                    {
                        "project": "TestProject",
                        "version": "v1.0.0",
                        "severity": "INFO",
                        "location": {
                            "file": "com.test.file",
                            "line": "10"
                        },
                        "timestamp": "${DateTime.now()}"
                    }
                """.trimIndent()
            )
        }

        coVerify(exactly = 1) {
            aggregateMock.aggregate(any())
        }
    }

    @Test
    fun `Received messages will not be aggregated if timestamp is missing and checked`() {
        val aggregateMock = mockk<AggregationManager>(relaxed = true)

        val intake = ZMQIntake(
            Configuration("", 0, 1, 1),
            aggregateMock,
            PipelinePluginConfiguration("Aggregator", "localhost:12346")
        )

        runBlocking {
            intake.onMessageReceived(
                """
                    {
                        "project": "TestProject",
                        "version": "v1.0.0",
                        "severity": "INFO",
                        "location": {
                            "file": "com.test.file",
                            "line": "10"
                        }
                    }
                """.trimIndent()
            )
        }

        coVerify(exactly = 0) {
            aggregateMock.aggregate(any())
        }
    }

    @Test
    fun `Received messages will not be aggregated if timestamp is present but mismatches granularity`() {
        val aggregateMock = mockk<AggregationManager>(relaxed = true)

        val intake = ZMQIntake(
            Configuration("", 0, 1, 1),
            aggregateMock,
            PipelinePluginConfiguration("Aggregator", "localhost:12346")
        )

        runBlocking {
            intake.onMessageReceived(
                """
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
            )
        }

        coVerify(exactly = 0) {
            aggregateMock.aggregate(any())
        }
    }

    @Test
    fun `Received messages will be aggregated with invalid timestamps if checking is disabled`() {
        val aggregateMock = mockk<AggregationManager>(relaxed = true)

        val intake = ZMQIntake(
            Configuration("", 0, 1, 1, false),
            aggregateMock,
            PipelinePluginConfiguration("Aggregator", "localhost:12346")
        )

        runBlocking {
            // outdated
            intake.onMessageReceived(
                """
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
            )

            // null
            intake.onMessageReceived(
                """
                    {
                        "project": "TestProject",
                        "version": "v1.0.0",
                        "severity": "INFO",
                        "location": {
                            "file": "com.test.file",
                            "line": "10"
                        }
                    }
                """.trimIndent()
            )
        }

        coVerify(exactly = 2) {
            aggregateMock.aggregate(any())
        }
    }

    @Test
    fun `Invalid messages will be ignored, without crashes`() {
        val aggregateMock = mockk<AggregationManager>(relaxed = true)

        val intake = ZMQIntake(
            mockk(),
            mockk(relaxed = true),
            PipelinePluginConfiguration("Aggregator", "localhost:12346")
        )

        // Project is missing
        runBlocking {
            intake.onMessageReceived(
                """
                    {
                        "version": "v1.0.0",
                        "severity": "INFO",
                        "location": {
                            "file": "com.test.file",
                            "line": "10"
                        },
                        "timestamp": "2020-05-07T11:22:00.644Z"
                    }
                """.trimIndent()
            )
        }

        coVerify(exactly = 0) {
            aggregateMock.aggregate(any())
        }
    }
}
