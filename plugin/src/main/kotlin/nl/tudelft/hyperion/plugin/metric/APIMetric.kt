package nl.tudelft.hyperion.plugin.metric

/**
 * Represents an element as the result of an /api/v1/metrics API call.
 * Contains the aggregated log counts over the specified interval, i.e.
 * an interval of 60 represents the logs that happened in the last minute.
 */
data class APIMetricsResult(
    val interval: Int,
    val versions: Map<String, List<APIMetric>>
)

/**
 * Represents a single grouped metric of a (project, file, line, version,
 * severity) tuple. Contains the line for which it was triggered, the
 * severity of the log and the amount of times that specific log was
 * triggered.
 */
data class APIMetric(
    val line: Int,
    val severity: String,
    val count: Int
)
