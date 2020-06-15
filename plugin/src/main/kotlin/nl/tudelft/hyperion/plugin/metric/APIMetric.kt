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
 * Represents the response from an /api/v1/metrics/period API call.
 *
 * @param T signifies if this is project wide or file specific bin metrics.
 * @property interval the time in seconds between each [APIBinMetricsResult].
 * @property results a list of metrics for each interval which contains a map
 *  of versions and corresponding line metrics.
 */
data class APIBinMetricsResponse<T : BaseAPIMetric>(
    val interval: Int,
    val results: List<APIBinMetricsResult<T>>
) {
    fun filterVersion(version: String, lineNumber: Int) =
        results.map { it.filterVersion(version, lineNumber) }
}

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
    val versions: MutableMap<String, List<T>>
) {
    fun filterVersion(version: String, lineNumber: Int) {
        if (version in versions) {
            versions[version] = versions[version]?.filter { it.line == lineNumber }!!
        }
    }
}

/**
 * The base class for all metrics.
 *
 * @property line the log line for which this metrics was triggered.
 * @property severity the log severity of this log line.
 * @property count the number of times this line has been triggered.
 */
sealed class BaseAPIMetric(
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
data class APIMetric(
    override val line: Int,
    override val severity: String,
    override val count: Int
) : BaseAPIMetric(line, severity, count)

/**
 * Represents a single grouped metric of a (project, file, line, version,
 * severity) tuple. Contains the line for which it was triggered, the
 * severity of the log and the amount of times that specific log was
 * triggered.
 */
data class FileAPIMetric(
    override val line: Int,
    override val severity: String,
    override val count: Int,
    val file: String
) : BaseAPIMetric(line, severity, count)
