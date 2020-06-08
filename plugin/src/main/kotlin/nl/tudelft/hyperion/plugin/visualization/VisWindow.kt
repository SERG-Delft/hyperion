@file:JvmName("VisWindow")

package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.plugin.connection.APIRequestor
import nl.tudelft.hyperion.plugin.graphs.HistogramData
import nl.tudelft.hyperion.plugin.graphs.InteractiveHistogram
import nl.tudelft.hyperion.plugin.metric.APIBinMetricsResponse
import nl.tudelft.hyperion.plugin.metric.BaseAPIMetric
import nl.tudelft.hyperion.plugin.settings.HyperionSettings
import nl.tudelft.hyperion.plugin.graphs.HistogramInterval
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.awt.Color
import java.awt.event.ItemEvent
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class VisWindow {
    lateinit var root: JPanel
    lateinit var main: JPanel
    lateinit var granularityComboBox: JComboBox<HistogramInterval>
    lateinit var onlyFileCheckBox: JCheckBox
    lateinit var statusLabel: JLabel
    lateinit var refreshButton: JButton
    lateinit var fileField: JTextField

    companion object {
        const val HISTOGRAM_X_MARGIN = 50
        const val HISTOGRAM_Y_MARGIN = 30
        const val HISTOGRAM_BAR_SPACING = 5

        // TODO: make color scheme configurable
        //  or make the unique severities in the aggregator unique
        private val HISTOGRAM_DEFAULT_COLOR: Color = Color.GRAY

        private val HISTOGRAM_COLOR_SCHEME = mapOf(
            "err" to Color.RED,
            "error" to Color.RED,
            "warn" to Color.ORANGE,
            "warning" to Color.ORANGE,
            "info" to Color.GREEN,
            "debug" to Color.BLUE
        )

        private val logger = Logger.getInstance(VisWindow::class.java)
        private val hyperionSettings = HyperionSettings.getInstance(ProjectManager.getInstance().openProjects[0])

        private val DATETIME_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("kk:mm:ss");

        fun parseAPIBinResponse(
            version: String,
            response: APIBinMetricsResponse<out BaseAPIMetric>
        ): HistogramData {
            // TODO: also add parsing for the severity label
            val bins = mutableListOf<Array<Int>>()
            val colors = mutableListOf<Array<Color>>()
            val severities = mutableListOf<Array<String>>()
            val timestamps = mutableListOf<String>()

            response.results.forEach {
                // Add formatted timestamp values per box
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

                val bin = it.versions[version] ?: error("version=$version removed at runtime")

                // Add the counts per box from the metrics
                bins.add(bin.map(BaseAPIMetric::count).toTypedArray())

                // Add the color per box from the metrics
                colors.add(
                    bin.map { metric ->
                        HISTOGRAM_COLOR_SCHEME.getOrDefault(metric.severity.toLowerCase(), HISTOGRAM_DEFAULT_COLOR)
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
    }

    val content
        get() = root

    fun createUIComponents() {
        granularityComboBox = ComboBox(HistogramInterval.values())
        granularityComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                val selectedItem = it.item as HistogramInterval
                // Store the interval in persistence
                hyperionSettings.state.visualization.interval = selectedItem
                runBlocking {
                    launch(Dispatchers.IO) {
                        val params = queryBinAPI("v1.0.0", selectedItem.relativeTime, 12)
                        val hist = (main as InteractiveHistogram)
                        hist.update(params)
                    }
                }
            }
        }

        fileField = JTextField()
        if (hyperionSettings.state.visualization.fileOnly) {
            fileField.isVisible = true
            fileField.text = hyperionSettings.state.visualization.filePath ?: ""
        } else {
            fileField.isVisible = false
        }

        onlyFileCheckBox = JCheckBox()
        onlyFileCheckBox.isSelected = hyperionSettings.state.visualization.fileOnly
        onlyFileCheckBox.addItemListener {
            val isSelected = it.stateChange == ItemEvent.SELECTED
            fileField.isVisible = isSelected
            hyperionSettings.state.visualization.fileOnly = isSelected
        }

        refreshButton = JButton()
        refreshButton.addActionListener {
            runBlocking {
                launch(Dispatchers.IO) {
                    val params = queryBinAPI("v1.0.0", 2400, 12)
                    val hist = (main as InteractiveHistogram)
                    hist.update(params)
                }
            }
        }

        main = createHistogramComponent()

        // runBlocking {
        //     launch(Dispatchers.IO) {
        //         val params = queryBinAPI("v1.0.0", 2400, 12)
        //         val hist = (main as InteractiveHistogram)
        //         hist.update(params)
        //     }
        // }
    }

    private suspend fun queryBinAPI(
        version: String,
        relativeTime: Int,
        steps: Int
    ): HistogramData {
        val project = ProjectManager.getInstance().openProjects[0]
        val data = APIRequestor.getBinnedMetrics(project, relativeTime, steps)

        return parseAPIBinResponse(version, data)
    }

    private fun createHistogramComponent(): InteractiveHistogram =
        InteractiveHistogram(
            HistogramData(
                arrayOf(
                    arrayOf(10),
                    arrayOf(10, 30, 5),
                    arrayOf(20, 20, 10, 5),
                    arrayOf(20, 15, 40, 5),
                    arrayOf(20, 15, 30, 5),
                    arrayOf(20, 15, 50, 5),
                    arrayOf(20, 15, 50, 5),
                    arrayOf(20, 15, 60, 5)
                ),
                arrayOf(
                    arrayOf(Color.RED),
                    arrayOf(Color.ORANGE, Color.GREEN, Color.BLUE),
                    arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE),
                    arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE),
                    arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE),
                    arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE),
                    arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE),
                    arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE)
                ),
                arrayOf(
                    arrayOf("ERROR"),
                    arrayOf("WARN", "INFO", "DEBUG"),
                    arrayOf("ERROR", "WARN", "INFO", "DEBUG"),
                    arrayOf("ERROR", "WARN", "INFO", "DEBUG"),
                    arrayOf("ERROR", "WARN", "INFO", "DEBUG"),
                    arrayOf("ERROR", "WARN", "INFO", "DEBUG"),
                    arrayOf("ERROR", "WARN", "INFO", "DEBUG"),
                    arrayOf("ERROR", "WARN", "INFO", "DEBUG")
                ),
                arrayOf("10:00:00", "10:00:05", "10:00:10", "10:00:15", "10:00:20", "10:00:25", "10:00:30", "10:00:35")
            ),
            HISTOGRAM_X_MARGIN,
            HISTOGRAM_Y_MARGIN,
            HISTOGRAM_BAR_SPACING
        )
}
