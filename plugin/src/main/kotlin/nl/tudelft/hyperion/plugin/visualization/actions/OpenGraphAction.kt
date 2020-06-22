package nl.tudelft.hyperion.plugin.visualization.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import nl.tudelft.hyperion.plugin.graphs.ProjectScope
import nl.tudelft.hyperion.plugin.settings.HyperionSettings
import nl.tudelft.hyperion.plugin.util.HyperionNotifier
import nl.tudelft.hyperion.plugin.visualization.VisToolWindowFactory

/**
 * Action for visualizing metrics of the entire project.
 */
class OpenGraphAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val currentProject = e.getData(CommonDataKeys.PROJECT)

        if (currentProject == null) {
            HyperionNotifier.error(currentProject, "Current project does not exist")
            return
        }

        val hyperionSettings = HyperionSettings.getInstance(currentProject)

        hyperionSettings.state.visualization.scope = ProjectScope

        // Open tool window if it exists
        ToolWindowManager
            .getInstance(currentProject)
            .getToolWindow("Visualization")
            ?.show {
                VisToolWindowFactory.histogramTab.updateAllSettings()
                VisToolWindowFactory.histogramTab.queryAndUpdate()
                VisToolWindowFactory.histogramTab.root.repaint()
            }
    }
}
