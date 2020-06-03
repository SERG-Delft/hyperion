package nl.tudelft.hyperion.aggregator.api

import nl.tudelft.hyperion.aggregator.Configuration
import nl.tudelft.hyperion.aggregator.database.AggregationEntries
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import kotlin.math.ceil
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
 * Represents an element as the result of an /api/v1/metrics API call.
 * Contains the aggregated log counts starting from [startTime] until
 * [startTime] plus the interval time.
 */
data class BinnedMetricsResult(
    val startTime: Long,
    val intervalLength: Int,
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
): List<MetricsResult> = intervals.map {
    // Clamp to values within bounds.
    val interval = max(min(it, configuration.aggregationTtl), configuration.granularity)
    val startTime = DateTime.now().minusSeconds(interval)
    val endTime = DateTime.now()

    transaction {
        val grouped = computeMetricsQuery(file, project, startTime, endTime)

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
}

/**
 * Compute aggregated on per-period basis for the specified project, file
 * and intervals. This will run async and return a job that can be awaited for the
 * result of the computation.
 */
fun computePeriodicMetrics(
    configuration: Configuration,
    project: String,
    file: String? = null,
    relativeTime: Int,
    steps: Int
): List<BinnedMetricsResult> {
    // Clamp to values within bounds.
    val clampedTime = min(relativeTime, configuration.aggregationTtl)
    val startTime = DateTime.now().minusSeconds(clampedTime)

    val interval = max(
        // Take the ceiling of the step size
        ceil(clampedTime.toFloat() / steps).toInt(),
        configuration.granularity
    )

    // Create the start time of each interval
    val startTimes = (0 until (clampedTime / interval)).map { startTime.plusSeconds(it * interval) }

    return startTimes.map { start ->
        transaction {
            val grouped = computeMetricsQuery(file, project, start, start.plusSeconds(interval))

            // Convert to expected format.
            BinnedMetricsResult(
                start.millis / 1000,
                interval,
                grouped.mapValues { (_, rows) ->
                    rows.map {
                        Metric(
                            it[AggregationEntries.line],
                            it[AggregationEntries.severity],
                            it[AggregationEntries.numTriggers.sum()]!!
                        )
                    }
                })
        }
    }
}

/**
 * Computes aggregated metrics from the database between the given intervals.
 *
 * @param file the name of the file to aggregate metrics of.
 *  Can optionally be null if querying project wide metrics.
 * @param project the name of the project to aggregate metrics of.
 * @param startTime starting timestamp of aggregated logs.
 * @param endTime ending timestamp of aggregated logs.
 */
fun computeMetricsQuery(
    file: String? = null,
    project: String,
    startTime: DateTime,
    endTime: DateTime
): Map<String, List<ResultRow>> =
    // Group on version
    AggregationEntries
        // SELECT version, line, severity, SUM(num_triggers)
        .slice(
            AggregationEntries.version,
            AggregationEntries.line,
            AggregationEntries.severity,
            AggregationEntries.numTriggers.sum()
        )
        // WHERE file = ? AND project = ?
        .select {
            val base =
                (AggregationEntries.project eq project) and
                    (AggregationEntries.timestamp greater startTime) and
                    (AggregationEntries.timestamp less endTime)

            // Add file check if not querying project wide statistics
            if (file != null) {
                return@select base and (AggregationEntries.file eq file)
            }

            base
        }
        // ORDER BY line ASC
        .orderBy(AggregationEntries.line)
        // GROUP BY version, severity, line
        .groupBy(AggregationEntries.version, AggregationEntries.severity, AggregationEntries.line)
        // group by version string
        .groupBy { it[AggregationEntries.version] }
