package nl.tudelft.hyperion.aggregator.api

import nl.tudelft.hyperion.aggregator.Configuration
import nl.tudelft.hyperion.aggregator.database.AggregationEntries
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import kotlin.math.max
import kotlin.math.min

/**
 * Represents an element as the result of an /api/v1/metrics API call.
 * Contains the aggregated log counts over the specified interval, i.e.
 * an interval of 60 represents the logs that happened in the last minute.
 */
data class MetricsResult(
    val interval: Int,
    val versions: Map<String, List<Metric>>
)

/**
 * Represents a single grouped metric of a (project, file, line, version,
 * severity) tuple. Contains the line for which it was triggered, the
 * severity of the log and the amount of times that specific log was
 * triggered.
 */
data class Metric(
    val line: Int,
    val severity: String,
    val count: Int
)

/**
 * Computes aggregated metrics from the database for the specified project, file
 * and intervals. This will run async and return a job that can be awaited for the
 * result of the computation.
 */
fun computeMetrics(
    configuration: Configuration,
    project: String,
    file: String,
    intervals: List<Int>
): List<MetricsResult> {
    return intervals.map {
        // Clamp to values within bounds.
        val interval = max(min(it, configuration.aggregationTtl), configuration.granularity)
        val startTime = DateTime.now().minusSeconds(interval)

        // Group on version
        val entries = transaction {
            val grouped = AggregationEntries
                // SELECT version, line, severity, SUM(num_triggers)
                .slice(
                    AggregationEntries.version,
                    AggregationEntries.line,
                    AggregationEntries.severity,
                    AggregationEntries.numTriggers.sum()
                )
                // WHERE file = ? AND project = ?
                .select {
                    (AggregationEntries.file eq file) and (AggregationEntries.project eq project) and
                        (AggregationEntries.timestamp greater startTime)
                }
                // GROUP BY version, severity, line
                .groupBy(AggregationEntries.version, AggregationEntries.severity, AggregationEntries.line)
                // group by version string
                .groupBy { it[AggregationEntries.version] }

            // Convert to expected format.
            MetricsResult(interval, grouped.mapValues { (_, rows) ->
                rows.map {
                    Metric(
                        it[AggregationEntries.line],
                        it[AggregationEntries.severity],
                        it[AggregationEntries.numTriggers.sum()]!!
                    )
                }
            })
        }

        entries
    }
}
