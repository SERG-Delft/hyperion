package nl.tudelft.hyperion.plugin.metric

import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder

/**
 * Represents all metrics for a given line.
 */
class LineMetrics(val metrics: List<IntervalMetric>) {

    fun getLine(): Int {
        if (metrics.isEmpty()) return -1

        return metrics.first().metric.line - 1
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
}
data class IntervalMetric(
        val interval: Int,
        val metric: Metric
) {

    private val formatter = PeriodFormatterBuilder()
            .appendWeeks().appendSuffix(" w").appendSeparator(" ")
            .appendDays().appendSuffix(" d").appendSeparator(" ")
            .appendHours().appendSuffix(" h").appendSeparator(" ")
            .appendMinutes().appendSuffix(" min").appendSeparator(" ")
            .appendSeconds().appendSuffix(" s").appendSeparator(" ")
            .toFormatter()

    fun getFormattedInterval(): String {
        return formatter.print(Period(interval * 1000L).normalizedStandard())
    }

    fun getText(): String {
        return "${metric.count} last ${getFormattedInterval()}"
    }
}
