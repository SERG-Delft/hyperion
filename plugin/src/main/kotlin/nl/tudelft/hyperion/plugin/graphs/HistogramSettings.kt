package nl.tudelft.hyperion.plugin.graphs

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.util.xmlb.Converter
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.warn

/**
 * Represents the scoping level of the metrics.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(ProjectScope::class),
    JsonSubTypes.Type(FileScope::class),
    JsonSubTypes.Type(LineScope::class)
)
sealed class MetricsScope

/**
 * MetricsScope that represents all log lines in all files.
 */
object ProjectScope : MetricsScope()

/**
 * MetricsScope that represents all log lines in a single file.
 *
 * @property filePath the path to the targeted file.
 */
open class FileScope(open val filePath: String) : MetricsScope()

/**
 * MetricsScope that represents a single log line in a single file.
 *
 * @property filePath the path to the targeted file.
 * @property line the visible line number of the targeted log line.
 */
data class LineScope(override val filePath: String, val line: Int) : FileScope(filePath)

/**
 * Represents the main settings object for all histogram visualization
 * settings.
 *
 * @property interval the currently used relative time interval.
 * @property timesteps the amount of bins to use.
 * @property scope the scope of the metrics.
 */
@JsonIgnoreProperties(value = ["fileOnly", "filePath"])
data class HistogramSettings(
    var interval: HistogramInterval,
    var timesteps: Int = 12,
    var scope: MetricsScope
) {
    /**
     * If the metrics only target a single file.
     */
    val fileOnly: Boolean
        get() = scope is FileScope || scope is LineScope

    /**
     * The path of the file if the scope of the metrics is file level.
     */
    val filePath: String?
        get() {
            return if (fileOnly) {
                (scope as FileScope).filePath
            } else {
                null
            }
        }
}

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
            .also {
                it.registerModule(KotlinModule())
                it.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
            }
    }

    @SuppressWarnings("TooGenericExceptionCaught")
    override fun fromString(value: String): HistogramSettings? {
        return try {
            serializer.readValue(value)
        } catch (e: Exception) {
            getLogger<HistogramSettingsConverter>().warn { "Could not deserialize settings, setting default" }
            HistogramSettings(
                HistogramInterval.HalfHour,
                12,
                ProjectScope
            )
        }
    }

    override fun toString(value: HistogramSettings): String? =
        serializer.writeValueAsString(value)
}
