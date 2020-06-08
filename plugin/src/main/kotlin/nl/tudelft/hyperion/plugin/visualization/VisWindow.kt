@file:JvmName("VisWindow")

package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.plugin.connection.APIRequestor
import nl.tudelft.hyperion.plugin.graphs.HistogramData
import nl.tudelft.hyperion.plugin.graphs.HistogramInterval
import nl.tudelft.hyperion.plugin.graphs.InteractiveHistogram
import nl.tudelft.hyperion.plugin.graphs.parseAPIBinResponse
import nl.tudelft.hyperion.plugin.settings.HyperionSettings
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.awt.Color
import java.awt.event.ItemEvent
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JTextField

class VisWindow {
    lateinit var root: JPanel
    lateinit var main: JPanel
    lateinit var granularityComboBox: JComboBox<HistogramInterval>
    lateinit var onlyFileCheckBox: JCheckBox
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
            "emerg" to Color.RED,
            "alert" to Color.RED,
            "crit" to Color.RED,
            "err" to Color.RED,
            "error" to Color.RED,
            "warn" to Color.ORANGE,
            "warning" to Color.ORANGE,
            "notive" to Color.GREEN,
            "info" to Color.GREEN,
            "debug" to Color.BLUE
        )

        private val visualizationSettings =
            HyperionSettings
                .getInstance(ProjectManager.getInstance().openProjects[0])
                .state
                .visualization

        private val DATETIME_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("kk:mm:ss\nMMM dd");
    }

    val content
        get() = root

    fun createUIComponents() {
        createGranularityComboBox()
        createFileField()
        createFileCheckBox()
        createRefreshButton()

        main = createHistogramComponent()
    }

    private fun createRefreshButton() {
        refreshButton = JButton()
        refreshButton.addActionListener {
            queryAndUpdate("v1.0.0")
        }
    }

    private fun createFileField() {
        fileField = JTextField()
        if (visualizationSettings.fileOnly) {
            fileField.isVisible = true
            fileField.text = visualizationSettings.filePath ?: ""
        } else {
            fileField.isVisible = false
        }
    }

    private fun createGranularityComboBox() {
        granularityComboBox = ComboBox(HistogramInterval.values())
        granularityComboBox.selectedItem = visualizationSettings.interval
        granularityComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                val selectedItem = it.item as HistogramInterval
                visualizationSettings.interval = selectedItem
                queryAndUpdate("v1.0.0")
            }
        }
    }

    private fun createFileCheckBox() {
        onlyFileCheckBox = JCheckBox()
        onlyFileCheckBox.isSelected = visualizationSettings.fileOnly
        onlyFileCheckBox.addItemListener {
            val isSelected = it.stateChange == ItemEvent.SELECTED
            fileField.isVisible = isSelected
            visualizationSettings.fileOnly = isSelected
        }
    }

    fun queryAndUpdate(version: String) = runBlocking {
        val project = ProjectManager.getInstance().openProjects[0]

        launch(Dispatchers.IO) {
            val params = if (visualizationSettings.fileOnly) {
                val data = APIRequestor.getBinnedMetrics(
                    visualizationSettings.filePath!!,
                    project,
                    visualizationSettings.interval.relativeTime,
                    visualizationSettings.timesteps
                )
                parseAPIBinResponse(version, DATETIME_FORMATTER, HISTOGRAM_COLOR_SCHEME, HISTOGRAM_DEFAULT_COLOR, data)
            } else {
                val data = APIRequestor.getBinnedMetrics(
                    project,
                    visualizationSettings.interval.relativeTime,
                    visualizationSettings.timesteps
                )
                parseAPIBinResponse(version, DATETIME_FORMATTER, HISTOGRAM_COLOR_SCHEME, HISTOGRAM_DEFAULT_COLOR, data)
            }
            val hist = (main as InteractiveHistogram)
            hist.update(params)
        }
    }

    fun updateAllSettings() {
        fileField.isVisible = visualizationSettings.fileOnly
        fileField.text = visualizationSettings.filePath ?: ""
        granularityComboBox.selectedItem = visualizationSettings.interval
        onlyFileCheckBox.isSelected = visualizationSettings.fileOnly
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
