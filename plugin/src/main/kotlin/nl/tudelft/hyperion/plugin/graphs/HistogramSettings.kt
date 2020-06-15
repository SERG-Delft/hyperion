package nl.tudelft.hyperion.plugin.graphs

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.util.xmlb.Converter

/**
 * Represents the main settings object for all histogram visualization
 * settings.
 *
 * @property interval the currently used relative time interval.
 * @property timesteps the amount of bins to use
 * @property fileOnly if the metrics are file only or project wide.
 * @property filePath the path to the file if [fileOnly] is true,
 *  otherwise null.
 */
data class HistogramSettings(
    var interval: HistogramInterval,
    var timesteps: Int = 12,
    var fileOnly: Boolean,
    var filePath: String? = null
)

/**
 * Represents the relative time intervals used for displaying the binned log
 * metrics.
 *
 * @property repr the string representation of this time interval.
 * @property relativeTime the time interval in seconds.
 */
enum class HistogramInterval(
    private val repr: String,
    val relativeTime: Int
) {
    HalfHour("Last 30 Minutes", 1800),
    Hour("Last Hour", 3600),
    QuarterDay("Last 6 Hours", 21600),
    HalfDay("Last 12 Hours", 43200),
    Day("Last Day", 86400),
    ThreeDays("Last 3 Days", 259200),
    Week("Last 7 Days", 604800);

    override fun toString(): String = repr
}

/**
 * Converter for transforming an [HistogramSettings] object to Intellij
 * compatible xml.
 *
 * Does this by serializing the string as a JSON formatted string.
 */
class HistogramSettingsConverter : Converter<HistogramSettings>() {
    companion object {
        val serializer = ObjectMapper(JsonFactory())
            .also { it.registerModule(KotlinModule()) }
    }

    override fun fromString(value: String): HistogramSettings? =
        serializer.readValue(value)

    override fun toString(value: HistogramSettings): String? =
        serializer.writeValueAsString(value)
}
