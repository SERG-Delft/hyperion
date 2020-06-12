package nl.tudelft.hyperion.plugin.visualization.components

import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.rd.util.debug
import com.jetbrains.rd.util.getLogger
import nl.tudelft.hyperion.plugin.graphs.ClickContext
import nl.tudelft.hyperion.plugin.metric.BaseAPIMetric
import nl.tudelft.hyperion.plugin.metric.FileAPIMetric
import nl.tudelft.hyperion.plugin.visualization.VisToolWindowFactory
import org.joda.time.DateTime
import java.nio.file.Paths

/**
 * Helper function for handling clicks inside the visualization tool window.
 * Updates information in the related lines tab when a bar or box is clicked.
 *
 * @param clickCtx information on where the user clicked inside the
 *  visualization.
 */
fun clickHandler(clickCtx: ClickContext) {
    getLogger<VisWindow>().debug { "Clicked with context $clickCtx" }

    val (newTitle, rows) =
        if (clickCtx.isWholeBarClicked) {
            showBinLineInfo(clickCtx.barIndex)
        } else {
            showBoxLineInfo(clickCtx)
        }
            ?: return

    ToolWindowManager
        .getInstance(VisWindow.ideProject)
        .getToolWindow("Visualization")
        ?.contentManager
        ?.setSelectedContent(VisToolWindowFactory.histogramContent)

    VisToolWindowFactory.codeListTab.updateTable(
        newTitle,
        rows
    )
}

/**
 * Click handler that returns information on the log lines that correspond to
 * the clicked histogram box inside of the visualization tool window.
 * The information is shown in a different tab.
 *
 * @param clickCtx information on where the user clicked inside the
 *  visualization.
 * @return a pair of the new title to put in the tab and the rows for the table
 *  where each row is a code line.
 */
private fun showBoxLineInfo(
    clickCtx: ClickContext
): Pair<String, List<CodeList.Companion.TableEntry>>? {
    val (newTitle, rows) = showBinLineInfo(clickCtx.barIndex) ?: return null
    val label = clickCtx.binComponent!!.label

    return Pair("$newTitle on $label", rows.filter { it.severity == label })
}

/**
 * Click handler that returns information on the log lines that correspond to
 * the clicked histogram bar inside of the visualization tool window.
 * The information is shown in a different tab.
 *
 * @param binIndex the index of which bin in the histogram was clicked.
 * @return a pair of the new title to put in the tab and the rows for the table
 *  where each row is a code line.
 */
private fun showBinLineInfo(binIndex: Int): Pair<String, List<CodeList.Companion.TableEntry>>? {
    val apiMetric = VisWindow.apiMetrics.results[binIndex].versions[VisWindow.branchVersion]

    if (apiMetric == null) {
        getLogger<VisWindow>().debug { "Version ${VisWindow.branchVersion} is missing from API metrics or it is empty" }
        return null
    }

    val rows = createdSortedTableEntries(apiMetric)

    val newTitle =
        if (rows.isEmpty()) {
            "No metrics available"
        } else {
            val startTime = VisWindow.apiMetrics.results[binIndex].startTime * 1000L
            createContextTitle(startTime = startTime)
        }

    return Pair(newTitle, rows)
}

/**
 * Converts either [FileAPIMetric] or [BaseAPIMetric] into table entries.
 *
 * @param apiMetric the list of metrics to convert.
 * @return the created list entries for the related lines tab.
 */
private fun createdSortedTableEntries(apiMetric: List<BaseAPIMetric>): List<CodeList.Companion.TableEntry> =
    if (VisWindow.settings.visualization.fileOnly) {
        apiMetric.map { m ->
            CodeList.Companion.TableEntry(
                VisWindow.settings.visualization.filePath!!,
                Paths.get(VisWindow.settings.visualization.filePath!!).fileName.toString(),
                m.line.toString(),
                m.severity,
                m.count.toString()
            )
        }
    } else {
        apiMetric.map { m ->
            check(m is FileAPIMetric)
            CodeList.Companion.TableEntry(
                m.file,
                Paths.get(m.file).fileName.toString(),
                m.line.toString(),
                m.severity,
                m.count.toString()
            )
        }
    }.sortedBy { it.triggerCount }

/**
 * Create the context title to display at the top of the related lines tab.
 */
private fun createContextTitle(startTime: Long): String {
    val startTimeString = DateTime(startTime).toString(VisWindow.DATETIME_FORMATTER_TIME)
    val endTimeString = DateTime(startTime + VisWindow.apiMetrics.interval * 1000L)
        .toString(VisWindow.DATETIME_FORMATTER_TIME)

    val contextText = if (VisWindow.settings.visualization.fileOnly) {
        VisWindow.settings.visualization.filePath!!
    } else {
        "all files"
    }

    return "Showing metrics for $contextText from $startTimeString to $endTimeString"
}
