package nl.tudelft.hyperion.plugin.metric

/**
 * Local data structure that represents an array of [APIMetricsResult].
 * Internally, it is grouped based on line, then interval, then a set of
 * versions and the amount of triggers in that specific version.
 */
data class FileMetrics(
    val lines: Map<Int, LineMetrics>
) {
    /**
     * Class that holds data used in the [fromMetricsResults] method.
     */
    data class MetricData(
        val line: Int,
        val interval: Int,
        val version: String,
        val count: Int
    )

    companion object {
        /**
         * Converts the specified set of results returned from the aggregator
         * into a [FileMetrics] object that represents those results.
         */
        fun fromMetricsResults(results: Array<APIMetricsResult>): FileMetrics {
            return FileMetrics(
                results.flatMap { result ->
                    result.versions.flatMap {
                        it.value.map { v -> MetricData(v.line, result.interval, it.key, v.count) }
                    }
                }.groupBy {
                    it.line
                }.mapValues {
                    mapToLineMetrics(it.value)
                }
            )
        }

        /**
         * Converts a
         * Map.Entry<line(Int), List<Pair<line(Int), Pair<interval(Int), Pair<version(String), count(Int)>>>>>
         * to LineMetrics.
         */
        private fun mapToLineMetrics(metricData: List<MetricData>): LineMetrics {
            return LineMetrics(
                metricData.groupBy {
                    it.interval
                }.mapValues {
                    it.value.map { m ->
                        LineIntervalMetric(m.version, m.count)
                    }
                }
            )
        }
    }
}

data class LineMetrics(
    val intervals: Map<Int, List<LineIntervalMetric>>
)

data class LineIntervalMetric(
    val version: String,
    val count: Int
)
