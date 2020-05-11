package nl.tudelft.hyperion.aggregator.workers

import io.mockk.every
import io.mockk.invoke
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.aggregator.Configuration
import nl.tudelft.hyperion.aggregator.api.LogEntry
import nl.tudelft.hyperion.aggregator.api.LogLocation
import nl.tudelft.hyperion.aggregator.database.AggregationEntries
import nl.tudelft.hyperion.aggregator.utils.TestWithoutLogging
import nl.tudelft.hyperion.aggregator.utils.withDisabledTransactions
import nl.tudelft.hyperion.aggregator.utils.withSpecificTransaction
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AggregationTest : TestWithoutLogging() {
    @Test
    fun `Aggregation commit worker should run at the granularity interval`() {
        val config = Configuration("a", 1, 1, 1)
        val aggregationManager = spyk(AggregationManager(config))

        every {
            aggregationManager["commit"]()
        } coAnswers {
            // Do nothing on commit.
        }

        // Run commit handler for 2.1 seconds. It should invoke the commit twice (delay, once, delay, twice)
        runBlocking {
            val worker = aggregationManager.startCommitWorker()
            delay(2100L)
            worker.cancel()
        }

        // Assert twice.
        verify(exactly = 2) {
            aggregationManager["commit"]()
        }
    }

    @Test
    fun `Aggregation commit worker should continue even when commit fails`() {
        val config = Configuration("a", 1, 1, 1)
        val aggregationManager = spyk(AggregationManager(config))

        every {
            aggregationManager["commit"]()
        } throws RuntimeException("Couldn't commit!")

        // Run commit handler for 2.1 seconds. It should invoke the commit twice (delay, once, delay, twice)
        runBlocking {
            val worker = aggregationManager.startCommitWorker()
            delay(2100L)
            worker.cancel()
        }

        // Assert twice.
        verify(exactly = 2) {
            aggregationManager["commit"]()
        }
    }

    @Test
    fun `Aggregation should reuse IntermediateAggregates when possible`() {
        val config = Configuration("a", 1, 1, 1)
        val aggregationManager = AggregationManager(config)

        val project1 = aggregationManager.getIntermediateAggregates("TestProject", "v1.0.0")
        val project2 = aggregationManager.getIntermediateAggregates("TestProject", "v1.0.0")
        val project3 = aggregationManager.getIntermediateAggregates("TestProject", "v1.0.1")

        Assertions.assertSame(project1, project2)
        Assertions.assertNotSame(project2, project3)
    }

    @Test
    fun `Aggregation should not reuse IntermediateAggregates across commits`() {
        val config = Configuration("a", 1, 1, 1)
        val aggregationManager = AggregationManager(config)

        // These should be equal to each other.
        val project1 = aggregationManager.getIntermediateAggregates("TestProject", "v1.0.0")
        val project2 = aggregationManager.getIntermediateAggregates("TestProject", "v1.0.0")

        // Run a commit.
        withDisabledTransactions {
            runBlocking {
                aggregationManager.commit()
            }
        }

        // These should not be equal to the previous ones.
        val project3 = aggregationManager.getIntermediateAggregates("TestProject", "v1.0.0")

        Assertions.assertSame(project1, project2)
        Assertions.assertNotSame(project2, project3)
    }

    @Test
    fun `Aggregation should reuse IntermediateAggregate when possible`() {
        val intermediateAggregates = IntermediateAggregates("TestProject", "v1.0.0")

        val result1 = intermediateAggregates.getIntermediateAggregate("A", 10, "INFO")
        val result2 = intermediateAggregates.getIntermediateAggregate("A", 10, "INFO")
        val result3 = intermediateAggregates.getIntermediateAggregate("A", 10, "WARN")
        val result4 = intermediateAggregates.getIntermediateAggregate("A", 11, "INFO")

        Assertions.assertSame(result1, result2)
        Assertions.assertSame(result2, result3)
        Assertions.assertNotSame(result3, result4)
    }

    @Test
    fun `Aggregation should not reuse IntermediateAggregate across commits`() {
        val intermediateAggregates = IntermediateAggregates("TestProject", "v1.0.0")

        val result1 = intermediateAggregates.getIntermediateAggregate("A", 10, "INFO")
        val result2 = intermediateAggregates.getIntermediateAggregate("A", 10, "INFO")

        // Run a commit.
        withDisabledTransactions {
            runBlocking {
                intermediateAggregates.commit()
            }
        }

        val result3 = intermediateAggregates.getIntermediateAggregate("A", 10, "INFO")

        Assertions.assertSame(result1, result2)
        Assertions.assertNotSame(result2, result3)
    }

    @Test
    fun `Aggregation should delegate to IntermediateAggregates`() {
        val config = Configuration("a", 1, 1, 1)
        val aggregationManager = spyk(AggregationManager(config))

        every {
            aggregationManager.getIntermediateAggregates(any(), any()).aggregate(any())
        } answers { }

        runBlocking {
            aggregationManager.aggregate(
                LogEntry(
                    "TestProject",
                    "v1.0.0",
                    "INFO",
                    LogLocation(
                        "com.test.file",
                        10
                    ),
                    DateTime.now()
                )
            )
        }

        // Assert that we delegated.
        verify(exactly = 1) {
            aggregationManager.getIntermediateAggregates(any(), any()).aggregate(any())
        }
    }

    @Test
    fun `IntermediateAggregates should commit to the database`() {
        val aggregates = IntermediateAggregates("TestProject", "v1.0.0")

        val base = LogEntry(
            "TestProject",
            "v1.0.0",
            "INFO",
            LogLocation(
                "com.test.file",
                10
            ),
            DateTime.now()
        )

        aggregates.aggregate(base)
        aggregates.aggregate(base)
        aggregates.aggregate(base.copy(location = base.location.copy(line = 12)))

        mockkStatic("org.jetbrains.exposed.sql.QueriesKt")

        every {
            AggregationEntries.batchInsert(any<Iterable<Any>>(), any<Boolean>(), captureLambda())
        } answers {
            // run block once (for code coverage, and checking that it doesn't crash)
            lambda<BatchInsertStatement.(Any) -> Unit>().invoke(
                mockk(relaxed = true),
                secondArg<Iterable<Any>>().first()
            )

            listOf()
        }

        withSpecificTransaction(mockk()) {
            aggregates.commit()
        }

        val baseInsert = AggregateInsertContainer("TestProject", "v1.0.0", "INFO", "com.test.file", 10, 2)

        verify(exactly = 1) {
            AggregationEntries.batchInsert(
                listOf(
                    baseInsert,
                    baseInsert.copy(line = 12, numTriggers = 1)
                ),
                any(),
                any()
            )
        }

        unmockkAll()
    }
}
