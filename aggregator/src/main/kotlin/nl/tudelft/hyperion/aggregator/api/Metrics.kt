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
    val versions: Map<String, List<BaseMetric>>
)

/**
 * Represents a base metric which can be extended with additional
 * fields.
 */
sealed class BaseMetric(
    open val line: Int,
    open val severity: String,
    open val count: Int
)

/**
 * Represents a single grouped metric of a (project, file, line, version,
 * severity) tuple. Contains the line for which it was triggered, the
 * severity of the log and the amount of times that specific log was
 * triggered.
 */
data class Metric(
    override val line: Int,
    override val severity: String,
    override val count: Int
) : BaseMetric(line, severity, count)

/**
 * Represents a metric with the originating file as an additional field.
 */
data class FileMetric(
    override val line: Int,
    override val severity: String,
    override val count: Int,
    val file: String
) : BaseMetric(line, severity, count)

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
        val grouped = computeMetricsQueryFile(file, project, startTime, endTime)

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
 * Compute aggregated on per-period basis for the specified project, file and
 * intervals. This will run async and return a job that can be awaited for the
 * result of the computation.
 *
 * @param configuration configuration of the plugin.
 * @param project the name of the project to aggregate metrics of.
 * @param file the name of the file to aggregate metrics of.
 *  Can optionally be null if querying project wide metrics.
 * @param relativeTime the relative time from current time to get metrics of.
 * @param steps the amount of steps to split the relative time in.
 *
 * @return pair of interval and results.
 */
fun computePeriodicMetrics(
    configuration: Configuration,
    project: String,
    file: String? = null,
    relativeTime: Int,
    steps: Int
): Pair<Int, List<BinnedMetricsResult>> {
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

    val results = startTimes.map { start ->
        transaction {
            val metrics = if (file == null) {
                // Retrieve project wide statistics
                val grouped = computeMetricsQuery(project, start, start.plusSeconds(interval))

                grouped.mapValues { (_, rows) ->
                    rows.map {
                        FileMetric(
                            it[AggregationEntries.line],
                            it[AggregationEntries.severity],
                            it[AggregationEntries.numTriggers.sum()]!!,
                            it[AggregationEntries.file]
                        )
                    }
                }
            } else {
                // Retrieve statistics of specific file
                val grouped = computeMetricsQueryFile(file, project, start, start.plusSeconds(interval))

                grouped.mapValues { (_, rows) ->
                    rows.map {
                        Metric(
                            it[AggregationEntries.line],
                            it[AggregationEntries.severity],
                            it[AggregationEntries.numTriggers.sum()]!!
                        )
                    }
                }
            }

            // Convert to expected format.
            BinnedMetricsResult(
                start.millis / 1000,
                metrics
            )
        }
    }

    return Pair(interval, results)
}

/**
 * Computes project wide aggregated metrics from the database between the
 * given intervals.
 *
 * @param project the name of the project to aggregate metrics of.
 * @param startTime starting timestamp of aggregated logs.
 * @param endTime ending timestamp of aggregated logs.
 */
fun computeMetricsQuery(
    project: String,
    startTime: DateTime,
    endTime: DateTime
): Map<String, List<ResultRow>> =
    // Group on version
    AggregationEntries
        // SELECT version, line, severity, file, SUM(num_triggers)
        .slice(
            AggregationEntries.version,
            AggregationEntries.line,
            AggregationEntries.severity,
            AggregationEntries.file,
            AggregationEntries.numTriggers.sum()
        )
        // WHERE project = ?
        .select {
            (AggregationEntries.project eq project) and
                (AggregationEntries.timestamp greater startTime) and
                (AggregationEntries.timestamp less endTime)
        }
        // ORDER BY line ASC
        .orderBy(AggregationEntries.line)
        // GROUP BY file, version, severity, line
        .groupBy(
            AggregationEntries.file,
            AggregationEntries.version,
            AggregationEntries.severity,
            AggregationEntries.line
        )
        // group by version string
        .groupBy { it[AggregationEntries.version] }

/**
 * Computes aggregated metrics from the database between the given intervals
 * for the given file.
 *
 * @param file the name of the file to aggregate metrics of.
 * @param project the name of the project to aggregate metrics of.
 * @param startTime starting timestamp of aggregated logs.
 * @param endTime ending timestamp of aggregated logs.
 */
fun computeMetricsQueryFile(
    file: String,
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
            (AggregationEntries.project eq project) and
                (AggregationEntries.timestamp greater startTime) and
                (AggregationEntries.timestamp less endTime) and
                (AggregationEntries.file eq file)
        }
        // ORDER BY line ASC
        .orderBy(AggregationEntries.line)
        // GROUP BY version, severity, line
        .groupBy(AggregationEntries.version, AggregationEntries.severity, AggregationEntries.line)
        // group by version string
        .groupBy { it[AggregationEntries.version] }
