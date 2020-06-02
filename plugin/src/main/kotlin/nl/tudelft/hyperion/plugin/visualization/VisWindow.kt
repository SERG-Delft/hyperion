@file:JvmName("VisWindow")

package nl.tudelft.hyperion.plugin.visualization

import nl.tudelft.hyperion.plugin.visualization.graphing.InteractiveHistogram
import java.awt.Color
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel

class VisWindow {
    lateinit var root: JPanel
    lateinit var label: JLabel
    lateinit var main: JPanel

    val content
        get() = root

    fun createUIComponents() {
        val model = DefaultTableModel(
            arrayOf(
                arrayOf(
                    "File",
                    "Class",
                    "Line",
                    "Count"
                ),
                arrayOf(
                    "src/main/java/com/sap/enterprises/server/impl/TransportationService.java",
                    "com.sap.enterprises.server.impl.TransportationService",
                    "11",
                    "250"
                ),
                arrayOf(
                    "src/main/java/org/sap/Sap.java",
                    "org.sap.Sap",
                    "10",
                    "42"
                )
            ), arrayOf("File", "Class", "Line", "Trigger Count")
        )

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
