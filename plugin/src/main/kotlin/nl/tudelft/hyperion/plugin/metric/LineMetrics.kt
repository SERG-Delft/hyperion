package nl.tudelft.hyperion.plugin.metric

import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder

/**
 * Represents all metrics for a given line.
 */
class LineMetrics(val metrics: List<IntervalMetric>) {
    val line
        get() = metrics.firstOrNull()?.metric?.line ?: -1

    val text
        get() = metrics.joinToString(" ") { "[${it.text}]" }
}

data class IntervalMetric(
    val interval: Int,
    val metric: Metric
) {
    companion object {
        private val formatter = PeriodFormatterBuilder()
            .appendWeeks().appendSuffix(" w").appendSeparator(" ")
            .appendDays().appendSuffix(" d").appendSeparator(" ")
            .appendHours().appendSuffix(" h").appendSeparator(" ")
            .appendMinutes().appendSuffix(" min").appendSeparator(" ")
            .appendSeconds().appendSuffix(" s").appendSeparator(" ")
            .toFormatter()
    }

    private val formattedInterval: String
        get() = formatter.print(Period(interval * 1000L).normalizedStandard())

    val text
        get() = "${metric.count} last $formattedInterval"
}
