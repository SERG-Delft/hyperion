package nl.tudelft.hyperion.plugin.graphs

import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.warn
import nl.tudelft.hyperion.plugin.metric.APIBinMetricsResponse
import nl.tudelft.hyperion.plugin.metric.BaseAPIMetric
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import java.awt.Color

/**
 * Short alias for 2D arrays.
 */
typealias Array2D<T> = Array<Array<T>>

/**
 * Represents all data necessary for a histogram, is composed of the counts for
 * each box, the list of timestamps, the label and color of each box
 *
 * TODO: make histogram data a single array of bin components
 */
data class HistogramData(
    var frequency: Array2D<Int>,
    var colors: Array2D<Color>,
    var labels: Array2D<String>,
    var timestamps: Array<String>
) {
    //<editor-fold desc="Hide generated">
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistogramData

        if (!frequency.contentDeepEquals(other.frequency)) return false
        if (!colors.contentDeepEquals(other.colors)) return false
        if (!labels.contentDeepEquals(other.labels)) return false
        if (!timestamps.contentEquals(other.timestamps)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = frequency.contentDeepHashCode()
        result = 31 * result + colors.contentDeepHashCode()
        result = 31 * result + labels.contentDeepHashCode()
        result = 31 * result + timestamps.contentHashCode()
        return result
    }
    //</editor-fold>
}

fun parseAPIBinResponse(
    version: String,
    timeFormatter: DateTimeFormatter,
    colorScheme: Map<String, Color>,
    defaultColor: Color,
    response: APIBinMetricsResponse<out BaseAPIMetric>
): HistogramData {
    val bins = mutableListOf<Array<Int>>()
    val colors = mutableListOf<Array<Color>>()
    val severities = mutableListOf<Array<String>>()
    val timestamps = mutableListOf<String>()

    response.results.forEach {
        // Add formatted timestamp values per box
        val endTime = it.startTime + response.interval
        timestamps.add(DateTime(endTime * 1000L).toString(timeFormatter))

        // Check if the given version exists
        if (version !in it.versions) {
            // TODO: add better missing version handling
            getLogger<HistogramData>().warn {
                "Version=$version missing from API response, setting count to 0"
            }

            bins.add(arrayOf())
            colors.add(arrayOf())
            severities.add(arrayOf())

            return@forEach
        }

        val bin = it.versions[version] ?: error("version=$version removed at runtime")

        // Add the counts per box from the metrics
        bins.add(bin.map(BaseAPIMetric::count).toTypedArray())

        // Add the color per box from the metrics
        colors.add(
            bin.map { metric ->
                colorScheme.getOrDefault(
                    metric.severity.toLowerCase(),
                    defaultColor
                )
            }.toTypedArray()
        )

        severities.add(bin.map(BaseAPIMetric::severity).toTypedArray())
    }

    return HistogramData(
        bins.toTypedArray(),
        colors.toTypedArray(),
        severities.toTypedArray(),
        timestamps.toTypedArray()
    )
}