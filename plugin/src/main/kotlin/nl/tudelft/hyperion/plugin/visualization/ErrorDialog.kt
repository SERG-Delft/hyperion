package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Represents a simple modal dialog that displays an error message.
 *
 * @property error the message to display.
 */
class ErrorDialog(private val error: String) : DialogWrapper(true) {

    init {
        init()
        title = "Error"
    }

    override fun createCenterPanel(): JComponent? {
        val dialogPanel = JPanel(BorderLayout())

        val label = JLabel(error)
        dialogPanel.add(label, BorderLayout.CENTER)

        return dialogPanel
    }

    override fun createActions(): Array<Action> {
        val closeAction = object : DialogWrapperExitAction("Close", CLOSE_EXIT_CODE) {}
        return arrayOf(closeAction)
    }
}

/**
 * Helper function for creating a new [ErrorDialog] and displaying it.
 *
 * @param error function to create the error message.
 */
fun errorDialog(error: () -> String) = ErrorDialog(error()).show()
