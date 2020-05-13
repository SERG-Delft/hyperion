package nl.tudelft.hyperion.plugin.metric

import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder

/**
 * Represents all metrics for a given line.
 */
class LineMetrics(private val metrics: List<IntervalMetric>) {

    fun getLine(): Int {
        if (metrics.isEmpty()) return -1

        return metrics.first().metric.line
    }

    fun setLine(line: Int) {
        for (lineMetric in metrics) {
            lineMetric.metric.line = line
        }
    }
    fun getText(): String {
        var result = ""

        for (metric in metrics) {
            result += "[${metric.getText()}]  "
        }

        return result.trimEnd(';', ' ')
    }
    override fun toString(): String {
        return metrics.toString()
    }
    data class IntervalMetric(
            val interval: Int,
            val metric: Metric
    ) {
        private val formatter = PeriodFormatterBuilder()
                .appendMonths().appendSuffix(" M")
                .appendWeeks().appendSuffix(" w")
                .appendDays().appendSuffix(" d")
                .appendHours().appendSuffix(" h")
                .appendMinutes().appendSuffix(" min")
                .appendSeconds().appendSuffix(" s")
                .toFormatter()

        fun getFormattedInterval(): String {
            return formatter.print(Period(interval * 1000L).normalizedStandard())
        }

        fun getText(): String {
            return "${metric.count} last ${getFormattedInterval()}".replace(" 1 ", " ")
        }
    }
}