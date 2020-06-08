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

        if (currentProject != null && currentFile != null) {
            // Set file path to current file
            HyperionSettings.getInstance(currentProject).state.visualization.filePath =
                currentFile.path

            // Open tool window if it exists
            ToolWindowManager
                .getInstance(currentProject)
                .getToolWindow("Visualization")
                ?.show(null)
        }
    }
}