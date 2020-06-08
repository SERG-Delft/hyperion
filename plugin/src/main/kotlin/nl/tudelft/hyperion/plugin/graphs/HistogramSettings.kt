package nl.tudelft.hyperion.plugin.graphs

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.util.xmlb.Converter

data class HistogramSettings(
    var interval: HistogramInterval,
    var timesteps: Int = 12,
    var fileOnly: Boolean,
    var filePath: String? = null
)

enum class HistogramInterval(
    private val repr: String,
    val relativeTime: Int
) {
    HalfHour("30M", 1800),
    Hour("1H", 3600),
    QuarterDay("6H", 21600),
    HalfDay("12H", 43200),
    Day("1D", 86400),
    ThreeDays("3D", 259200);

    override fun toString(): String = repr
}

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
