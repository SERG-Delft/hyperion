package nl.tudelft.hyperion.plugin.metric

/**
 * Represents an element as the result of an /api/v1/metrics API call.
 * Contains the aggregated log counts over the specified interval, i.e.
 * an interval of 60 represents the logs that happened in the last minute.
 */
data class MetricsResult<T>(
        val interval: Int,
        val versions: Map<String, List<T>>
)