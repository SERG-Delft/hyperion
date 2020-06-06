package nl.tudelft.hyperion.plugin.graphs

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
