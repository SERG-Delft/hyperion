@file:JvmName("CodeList")

package nl.tudelft.hyperion.plugin.visualization.components

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.table.JBTable
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

/**
 * Represents a tab in tool window that displays information on code lines that
 * have logs and their metrics.
 */
class CodeList {
    lateinit var metricsTable: JTable
    lateinit var root: JPanel
    lateinit var label: JLabel

    lateinit var tableData: List<TableEntry>

    /**
     * Represents a mouse listener for [metricsTable] that opens the clicked
     * code line in a new editor.
     */
    private val metricsTableListener = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) {
            val row = metricsTable.rowAtPoint(e!!.point)

            // Clicked on the table while metrics have not been updated yet
            // ignore this exception
            if (!this@CodeList::tableData.isInitialized) {
                return
            }

            if (row > tableData.size) {
                throw IllegalStateException("Clicked on row that has missing data")
            }

            ProjectManager.getInstance().openProjects[0].let {
                val file = File(it.basePath!!)
                val baseDir: VirtualFile? = LocalFileSystem.getInstance().findFileByIoFile(file)
                val targetFile = baseDir?.findFileByRelativePath(tableData[row].path)

                // XXX: Naive assumption that the line has not moved and that column is 0
                OpenFileDescriptor(it, targetFile!!, tableData[row].lineNr.toInt() - 1, 0).navigate(true)
            }
        }
    }

    companion object {
        data class TableEntry(
            val path: String,
            val file: String,
            val lineNr: String,
            val severity: String,
            val triggerCount: String
        )
    }

    init {
        metricsTable.addMouseListener(metricsTableListener)
    }

    val content
        get() = root

    /**
     * Entry point for the Swing UI creation.
     */
    fun createUIComponents() {
        metricsTable = JBTable(
            DefaultTableModel(
                arrayOf("Path", "File", "Line Number", "Severity", "Trigger Count"),
                0
            )
        )
    }

    /**
     * Clears and updates [metricsTable] with the given table entries.
     *
     * @param title the new title to put at the top of the table.
     * @param tableData the new table entries.
     */
    fun updateTable(title: String, tableData: List<TableEntry>) {
        this.tableData = tableData

        // Update title
        label.text = title

        // Clear the data in the table
        val model = metricsTable.model as DefaultTableModel
        model.rowCount = 0

        for (entry in tableData) {
            model.addRow(arrayOf(entry.path, entry.file, entry.lineNr, entry.severity, entry.triggerCount))
        }
    }
}
