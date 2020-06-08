package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import nl.tudelft.hyperion.plugin.settings.HyperionSettings

class OpenGraphAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val currentProject = e.getData(CommonDataKeys.PROJECT)

        if (currentProject == null) {
            ErrorDialog("Current project does not exist").show()
            return
        }

        val hyperionSettings = HyperionSettings.getInstance(currentProject)

        hyperionSettings.state.visualization.fileOnly = false

        // TODO: remove hardcoded version
        VisWindowFactory.histogramTab.updateAllSettings()
        VisWindowFactory.histogramTab.queryAndUpdate("v1.0.0")

        // Open tool window if it exists
        ToolWindowManager
            .getInstance(currentProject)
            .getToolWindow("Visualization")
            ?.apply {
                this.component.repaint()
                show(null)
            }
    }
}