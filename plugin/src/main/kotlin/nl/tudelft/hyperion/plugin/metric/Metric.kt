package nl.tudelft.hyperion.plugin.metric

/**
 * Local data structure that represents an array of [APIMetricsResult].
 * Internally, it is grouped based on line, then interval, then a set of
 * versions and the amount of triggers in that specific version.
 */
data class FileMetrics(
    val lines: Map<Int, LineMetrics>
) {
    companion object {
        /**
         * Converts the specified set of results returned from the aggregator
         * into a [FileMetrics] object that represents those results.
         */
        fun fromMetricsResults(results: Array<APIMetricsResult>): FileMetrics {
            return FileMetrics(
                results.flatMap { result ->
                    result.versions.flatMap {
                        it.value.map { v -> v.line to (result.interval to (it.key to v.count)) }
                    }
                }.groupBy {
                    it.first
                }.mapValues {
                    LineMetrics(
                        it.value.map {
                            it.second
                        }.groupBy {
                            it.first
                        }.mapValues {
                            it.value.map { it.second }
                        }.mapValues {
                            it.value.map { LineIntervalMetric(it.first, it.second) }
                        }
                    )
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
