@file:JvmName("VisWindow")

package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.plugin.connection.APIRequestor
import nl.tudelft.hyperion.plugin.graphs.InteractiveHistogram
import nl.tudelft.hyperion.plugin.metric.APIBinMetricsResponse
import nl.tudelft.hyperion.plugin.metric.APIBinMetricsResult
import nl.tudelft.hyperion.plugin.metric.BaseAPIMetric
import nl.tudelft.hyperion.plugin.metric.FileAPIMetric
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.awt.Color
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel

class VisWindow {
    lateinit var root: JPanel
    lateinit var main: JPanel
    lateinit var granularityComboBox: JComboBox<String>
    lateinit var onlyFileCheckBox: JCheckBox
    lateinit var statusLabel: JLabel

    companion object {
        const val HISTOGRAM_X_MARGIN = 50
        const val HISTOGRAM_BAR_SPACING = 5

        // the start is from down up, so start > end
        // the y coordinates go from top to bottom
        const val HISTOGRAM_Y_START = 200
        const val HISTOGRAM_Y_END = 100

        // TODO: make color scheme configurable
        //  or make the unique severities in the aggregator unique
        val HISTOGRAM_DEFAULT_COLOR = Color.GRAY

        val HISTOGRAM_COLOR_SCHEME = mapOf(
            "err" to Color.RED,
            "error" to Color.RED,
            "warn" to Color.ORANGE,
            "warning" to Color.ORANGE,
            "info" to Color.GREEN,
            "debug" to Color.BLUE
        ).withDefault { HISTOGRAM_DEFAULT_COLOR }

        val logger = Logger.getInstance(VisWindow::class.java)

        private val DATETIME_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("kk:mm:ss");

        fun parseAPIBinResponse(
            version: String,
            response: APIBinMetricsResponse<out BaseAPIMetric>
        ): Triple<Array<Array<Int>>, Array<String>, Array<Array<Color>>> {
            // TODO: also add parsing for the severity label
            val bins = mutableListOf<Array<Int>>()
            val timestamps = mutableListOf<String>()
            val colors = mutableListOf<Array<Color>>()

            response.results.forEach {
                // Add formatted timestamp values
                val endTime = it.startTime + response.interval
                timestamps.add(DateTime(endTime * 1000L).toString(DATETIME_FORMATTER))

                // Check if the given version exists
                if (version !in it.versions) {
                    // TODO: add better missing version handling
                    logger.warn("Version=$version missing from API response, setting count to 0")

                    bins.add(arrayOf())
                    colors.add(arrayOf())

                    return@forEach
                }

                val bin = it.versions[version] ?: error("Runtime error: version=$version removed at runtime")

                // Add the counts from the metrics
                bins.add(bin.map(BaseAPIMetric::count).toTypedArray())

                // Add the color per box
                colors.add(
                    bin.map { metric ->
                        HISTOGRAM_COLOR_SCHEME[metric.severity]
                            ?: error("Runtime error: default is set but missing?")
                    }.toTypedArray()
                )
            }

            return Triple(bins.toTypedArray(), timestamps.toTypedArray(), colors.toTypedArray())
        }

        private fun getUniqueSeverities(version: String, results: List<APIBinMetricsResult<*>>) =
            if (results.all { it.versions.isEmpty() }) {
                0
            } else {
                val allSeverities = results
                    .filter { version in it.versions }
                    .map { it.versions[version] }
                    .flatMap { it!! }
                    .map(BaseAPIMetric::severity)

                if (allSeverities.isEmpty()) {
                    0
                } else {
                    allSeverities.toSet().size
                }
            }
    }

    val content
        get() = root

    fun createUIComponents() {
        val project = ProjectManager.getInstance().openProjects[0]

        var data: APIBinMetricsResponse<FileAPIMetric>?
        runBlocking {
            data = APIRequestor.getBinnedMetrics(project, 2400, 12)
            data!!.results.map { println(it) }
        }

        main = InteractiveHistogram(
            arrayOf(
                arrayOf(10, 10, 20, 4),
                arrayOf(40, 10, 30, 5),
                arrayOf(20, 20, 10, 5),
                arrayOf(20, 15, 40, 5),
                arrayOf(20, 15, 30, 5),
                arrayOf(20, 15, 50, 5),
                arrayOf(20, 15, 50, 5),
                arrayOf(20, 15, 60, 5)
            ),
            50,
            200, 100,
            10,
            arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE),
            arrayOf("ERROR", "WARN", "INFO", "DEBUG"),
            arrayOf("10:00:00", "10:00:05", "10:00:10", "10:00:15", "10:00:20", "10:00:25", "10:00:30", "10:00:35")
        )
    }
}
