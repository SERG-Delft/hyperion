package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import nl.tudelft.hyperion.plugin.settings.HyperionSettings

class OpenFileGraphAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val currentProject = e.getData(CommonDataKeys.PROJECT)
        val currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE)

        if (currentProject == null || currentFile == null) {
            ErrorDialog("Current open file is not linked to a project").show()
            return
        }

        if (!currentFile.path.startsWith(currentProject.basePath!!)) {
            ErrorDialog("file $currentFile is not in project ${currentProject.name}").show()
            return
        }

        // TODO: make path finding more robust
        // This might fail on remote projects
        val relativePath = currentFile.path.removePrefix("${currentProject.basePath!!}/")

        val hyperionSettings = HyperionSettings.getInstance(currentProject)

        // Set file path to current file
        hyperionSettings.state.visualization.filePath = relativePath
        hyperionSettings.state.visualization.fileOnly = true

        // Open tool window if it exists
        ToolWindowManager
            .getInstance(currentProject)
            .getToolWindow("Visualization")
            ?.show(null)

        VisWindowFactory.histogramTab.updateAllSettings()
        VisWindowFactory.histogramTab.queryAndUpdate()
        VisWindowFactory.histogramTab.root.repaint()
    }
}