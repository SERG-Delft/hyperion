package nl.tudelft.hyperion.plugin.metric

/**
 * Represents an element as the result of an /api/v1/metrics API call.
 * Contains the aggregated log counts grouped by line number
 * and version.
 */
data class MetricsResult(
        val versions: Map<String, List<LineMetrics>>
)
