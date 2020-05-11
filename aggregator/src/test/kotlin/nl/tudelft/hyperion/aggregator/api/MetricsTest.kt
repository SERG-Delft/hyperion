package nl.tudelft.hyperion.aggregator.api

import nl.tudelft.hyperion.aggregator.Configuration
import nl.tudelft.hyperion.aggregator.database.AggregationEntries
import nl.tudelft.hyperion.aggregator.utils.TestWithoutLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.joda.time.DateTimeUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.charset.Charset
import java.sql.Connection

class MetricsTest : TestWithoutLogging() {
    lateinit var transaction: Transaction

    @BeforeEach
    fun `Setup SQLite database with predefined data`() {
        val db = Database.connect("jdbc:sqlite::memory:")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction = db.transactionManager.newTransaction()

        transaction {
            SchemaUtils.create(AggregationEntries)

            // Load statements and execute.
            val statements = String(
                javaClass.classLoader.getResourceAsStream("aggregates.sql")!!
                    .readAllBytes(), Charset.defaultCharset()
            )

            for (stmt in statements.split("\n")) {
                exec(stmt)
            }
        }

        // Hardcode timestamp
        DateTimeUtils.setCurrentMillisFixed(1588844616804);
    }

    @AfterEach
    fun `Reset timestamp and database`() {
        DateTimeUtils.setCurrentMillisSystem()

        transaction.commit()
    }

    @Test
    fun `computeMetrics should clamp to the minimum granularity`() {
        val config = Configuration("a", 1, 20, 1000)

        // 1 second should yield no result. 20 seconds should yield results.
        // ie, if we clamp we should have results
        val results = computeMetrics(
            config,
            "TestProject",
            "com.sap.enterprises.server.impl.TransportationService",
            listOf(1)
        )

        Assertions.assertFalse(results[0].versions.isEmpty())
    }

    @Test
    fun `computeMetrics should aggregate across the specified timeframe`() {
        val config = Configuration("a", 1, 20, 1000)

        // 1 second should yield no result. 20 seconds should yield results.
        // ie, if we clamp we should have results
        val results = computeMetrics(
            config,
            "TestProject",
            "com.sap.enterprises.server.impl.TransportationService",
            listOf(20)
        )

        Assertions.assertEquals(
            results,
            listOf(
                MetricsResult(
                    interval = 20, versions = mapOf(
                        "v1.0.0" to listOf(
                            Metric(line = 11, severity = "INFO", count = 15),
                            Metric(line = 37, severity = "INFO", count = 3),
                            Metric(line = 20, severity = "WARN", count = 3)
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `computeMetrics should allow multiple timeframes`() {
        val config = Configuration("a", 1, 20, 1000)

        // 1 second should yield no result. 20 seconds should yield results.
        // ie, if we clamp we should have results
        val results = computeMetrics(
            config,
            "TestProject",
            "com.sap.enterprises.server.impl.TransportationService",
            listOf(20, 120)
        )

        Assertions.assertEquals(
            results,
            listOf(
                MetricsResult(
                    interval = 20, versions = mapOf(
                        "v1.0.0" to listOf(
                            Metric(line = 11, severity = "INFO", count = 20),
                            Metric(line = 37, severity = "INFO", count = 4),
                            Metric(line = 20, severity = "WARN", count = 4)
                        )
                    )
                ),
                MetricsResult(
                    interval = 120, versions = mapOf(
                        "v1.0.0" to listOf(
                            Metric(line = 23, severity = "ERROR", count = 16),
                            Metric(line = 34, severity = "ERROR", count = 28),
                            Metric(line = 11, severity = "INFO", count = 144),
                            Metric(line = 37, severity = "INFO", count = 84),
                            Metric(line = 20, severity = "WARN", count = 36)
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `computeMetrics should not error on missing files or projects`() {
        val config = Configuration("a", 1, 20, 1000)

        // Check invalid project
        var results = computeMetrics(
            config,
            "InvalidProject",
            "com.sap.enterprises.server.impl.TransportationService",
            listOf(1)
        )

        Assertions.assertTrue(results[0].versions.isEmpty())

        // Check invalid file.
        results = computeMetrics(
            config,
            "TestProject",
            "com.this.file.does.not.exist",
            listOf(1)
        )

        Assertions.assertTrue(results[0].versions.isEmpty())
    }
}
