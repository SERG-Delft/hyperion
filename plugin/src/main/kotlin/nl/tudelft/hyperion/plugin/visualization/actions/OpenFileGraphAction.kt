package nl.tudelft.hyperion.plugin.visualization.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import nl.tudelft.hyperion.plugin.graphs.FileScope
import nl.tudelft.hyperion.plugin.settings.HyperionSettings
import nl.tudelft.hyperion.plugin.util.HyperionNotifier
import nl.tudelft.hyperion.plugin.visualization.VisToolWindowFactory

/**
 * Action for visualizing metrics of the currently open file.
 */
class OpenFileGraphAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val currentProject = e.getData(CommonDataKeys.PROJECT)
        val currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE)

        if (currentProject == null || currentFile == null) {
            HyperionNotifier.error(
                currentProject,
                "Current open file is not linked to a project"
            )
            return
        }

        if (!currentFile.path.startsWith(currentProject.basePath!!)) {
            HyperionNotifier.error(
                currentProject,
                "file $currentFile is not in project ${currentProject.name}"
                )
            return
        }

        // TODO: make path finding more robust
        // This might fail on remote projects
        val relativePath = currentFile.path.removePrefix("${currentProject.basePath!!}/")

        val hyperionSettings = HyperionSettings.getInstance(currentProject)

        // Set file path to current file
        hyperionSettings.state.visualization.scope = FileScope(relativePath)

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
