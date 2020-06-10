@file:JvmName("VisWindow")

package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.plugin.connection.APIRequestor
import nl.tudelft.hyperion.plugin.git.GitVersionResolver
import nl.tudelft.hyperion.plugin.graphs.HistogramData
import nl.tudelft.hyperion.plugin.graphs.HistogramInterval
import nl.tudelft.hyperion.plugin.graphs.InteractiveHistogram
import nl.tudelft.hyperion.plugin.graphs.parseAPIBinResponse
import nl.tudelft.hyperion.plugin.settings.HyperionSettings
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.awt.Color
import java.awt.event.ItemEvent
import java.nio.file.Paths
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Represents the main tab of the plugin's Visualization tool window.
 * It shows a single graph with some controls for changing the parameters of
 * the graph, the parameters are linked with those from the currently open
 * project's [HyperionSettings.State].
 *
 * The graph's view should not be updated manually, but by calling
 * [queryAndUpdate], which uses [HyperionSettings.State].
 */
class VisWindow {
    lateinit var root: JPanel
    lateinit var main: JPanel
    lateinit var granularityComboBox: JComboBox<HistogramInterval>
    lateinit var barCountComboBox: JComboBox<CustomTextItem<Int>>
    lateinit var refreshButton: JButton
    lateinit var countLabel: JLabel
    lateinit var titleLabel: JLabel

    companion object {
        const val HISTOGRAM_X_MARGIN = 50
        const val HISTOGRAM_Y_MARGIN = 30
        const val HISTOGRAM_BAR_SPACING = 5

        // TODO: make color scheme configurable
        //  or make the severities in the aggregator unique
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

        private val DATETIME_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("kk:mm:ss\nMMM dd")

        private var ideProject: Project = ProjectManager.getInstance().openProjects[0]
            get() {
                if (field.isDisposed) {
                    field = ProjectManager.getInstance().openProjects[0]
                }

                return field
            }

        private val settings: HyperionSettings.State
            get() = HyperionSettings.getInstance(ideProject).state

        data class CustomTextItem<T>(
            val v: T,
            val format: String
        ) {
            override fun toString(): String = format.format(v)
        }

        private val TIME_STEPS = arrayOf(1, 2, 4, 6, 12, 24).map { CustomTextItem(it, "%s bars") }.toTypedArray()
    }

    val content
        get() = root

    /**
     * Creates all Swing components, is called by Intellij.
     */
    fun createUIComponents() {
        createGranularityComboBox()
        createRefreshButton()
        createBarCountComboBox()
        createHistogramComponent()

        // TODO: remove mock values later
        (main as InteractiveHistogram).update(createMockData())
    }

    /**
     * Creates the refresh button that executes the API call and repaints the
     * histogram when clicked.
     */
    private fun createRefreshButton() {
        refreshButton = JButton()
        refreshButton.addActionListener {
            queryAndUpdate()
        }
    }

    /**
     * Creates the granularity combobox that updates the settings when changed.
     */
    private fun createGranularityComboBox() {
        granularityComboBox = ComboBox(HistogramInterval.values())
        granularityComboBox.selectedItem = settings.visualization.interval
        granularityComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                val selectedItem = it.item as HistogramInterval
                settings.visualization.interval = selectedItem
                queryAndUpdate()
            }
        }
    }

    /**
     * Creates the bar count combobox that updates the settings when changed.
     */
    private fun createBarCountComboBox() {
        barCountComboBox = ComboBox(TIME_STEPS)
        // TODO: Add restrictions on legal time steps
        barCountComboBox.selectedItem = TIME_STEPS.find { it.v == settings.visualization.timesteps }!!
        barCountComboBox.addItemListener {
            if (it.stateChange == ItemEvent.SELECTED) {
                val selectedItem = it.item as CustomTextItem<*>
                settings.visualization.timesteps = selectedItem.v as Int
                queryAndUpdate()
            }
        }
    }

    /**
     * Queues an API call for binned metrics in an IO thread, of which the
     * results are used to update the histogram data and repaint the histogram
     * component.
     */
    fun queryAndUpdate() = runBlocking {
        launch(Dispatchers.IO) {
            val version = GitVersionResolver.getCurrentBranchHash(ideProject)

            requireNotNull(version) {
                "Could not retrieve the version of this project, which is the current hash of origin/HEAD"
            }

            val data = APIRequestor.getBinnedMetrics(
                settings.address,
                settings.project,
                settings.visualization.interval.relativeTime,
                settings.visualization.timesteps,
                if (settings.visualization.fileOnly) settings.visualization.filePath else null
            )

            val params = parseAPIBinResponse(
                version,
                DATETIME_FORMATTER,
                HISTOGRAM_COLOR_SCHEME,
                HISTOGRAM_DEFAULT_COLOR,
                data
            )

            val hist = (main as InteractiveHistogram)
            hist.update(params)
            countLabel.text = "${params.frequency.map { it.sum() }.sum()} Total Lines"
        }
    }

    /**
     * Sets all Swing component values to the corresponding values set in the
     * [settings] property.
     *
     */
    fun updateAllSettings() {
        granularityComboBox.selectedItem = settings.visualization.interval
        if (settings.visualization.fileOnly) {
            // Assume that filename is a local path
            // TODO: fix assumption that filePath is local
            val filename = Paths.get(settings.visualization.filePath!!).fileName.toString()
            titleLabel.text = "Showing metrics for $filename"
        } else {
            titleLabel.text = "Showing metrics for all files"
        }
    }

    /**
     * Creates an empty [InteractiveHistogram] component.
     */
    private fun createHistogramComponent() {
        main = InteractiveHistogram(
            HistogramData(
                arrayOf(arrayOf()),
                arrayOf(),
                arrayOf(arrayOf()),
                arrayOf()
            ),
            HISTOGRAM_X_MARGIN,
            HISTOGRAM_Y_MARGIN,
            HISTOGRAM_BAR_SPACING
        )
    }

    private fun createMockData(): HistogramData = HistogramData(
        arrayOf(
            arrayOf(10),
            arrayOf(10, 30, 5),
            arrayOf(),
            arrayOf(20, 15, 40, 5),
            arrayOf(20, 15, 30, 5),
            arrayOf(20, 15, 50, 5),
            arrayOf(20, 15, 50, 5),
            arrayOf(20, 15, 60, 5)
        ),
        arrayOf(
            arrayOf(Color.RED),
            arrayOf(Color.ORANGE, Color.GREEN, Color.BLUE),
            arrayOf(),
            arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE),
            arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE),
            arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE),
            arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE),
            arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE)
        ),
        arrayOf(
            arrayOf("ERROR"),
            arrayOf("WARN", "INFO", "DEBUG"),
            arrayOf(),
            arrayOf("ERROR", "WARN", "INFO", "DEBUG"),
            arrayOf("ERROR", "WARN", "INFO", "DEBUG"),
            arrayOf("ERROR", "WARN", "INFO", "DEBUG"),
            arrayOf("ERROR", "WARN", "INFO", "DEBUG"),
            arrayOf("ERROR", "WARN", "INFO", "DEBUG")
        ),
        arrayOf("10:00:00", "10:00:05", "10:00:10", "10:00:15", "10:00:20", "10:00:25", "10:00:30", "10:00:35")
    )
}
