package nl.tudelft.hyperion.plugin.metric

import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder

/**
 * Represents all metrics for a given line.
 */
class LineMetrics(val metrics: List<IntervalMetric>) {

    data class IntervalMetric(
            val interval: Int,
            val metric: Metric
    ) {
        private val formatter = PeriodFormatterBuilder()
                .appendMonths().appendSuffix("M")
                .appendWeeks().appendSuffix("w")
                .appendDays().appendSuffix("d")
                .appendHours().appendSuffix("h")
                .appendMinutes().appendSuffix("min")
                .appendSeconds().appendSuffix("s")
                .toFormatter()

        fun getIntervalFormat(): String {
            return formatter.print(Period(interval * 1000L).normalizedStandard())
        }
    }
}