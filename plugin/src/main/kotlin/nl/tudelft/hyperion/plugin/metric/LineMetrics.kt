package nl.tudelft.hyperion.plugin.metric

/**
 * Represents all metrics for a given line.
 */
class LineMetrics(val metrics: List<IntervalMetric>) {

    data class IntervalMetric(
            val interval: Int,
            val metric: Metric
    )
}