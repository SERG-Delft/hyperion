package nl.tudelft.hyperion.plugin.metric

/**
 * Represents an element as the result of an /api/v1/metrics API call.
 * Contains the aggregated log counts over the specified interval, i.e.
 * an interval of 60 represents the logs that happened in the last minute.
 */
data class APIMetricsResult(
    val interval: Int,
    /**
     * A mapping of version to a List of Metrics (all metrics relative to this version).
     * The version is represented by a String.
     */
    val versions: Map<String, List<APIMetric>>
)

/**
 * Represents an element as the result of an /api/v1/metrics/period API call.
 * The API call returns a list of bins where each bin is the aggregated log
 * counts starting from the given start time.
 *
 * @param T signifies if this is project wide or file specific bin metrics.
 * @property startTime unix epoch time in seconds of the starting time of this
 *  bin.
 * @property versions map of version identifier with metrics.
 */
data class APIBinMetricsResult<T : BaseAPIMetric>(
    val startTime: Int,
    val versions: Map<String, List<T>>
)

data class APIBinMetricsResponse<T : BaseAPIMetric>(
    val interval: Int,
    val results: List<APIBinMetricsResult<T>>
)

sealed class BaseAPIMetric

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
) : BaseAPIMetric()

/**
 * Represents a single grouped metric of a (project, file, line, version,
 * severity) tuple. Contains the line for which it was triggered, the
 * severity of the log and the amount of times that specific log was
 * triggered.
 */
data class FileAPIMetric(
    val line: Int,
    val severity: String,
    val count: Int,
    val file: String
) : BaseAPIMetric()