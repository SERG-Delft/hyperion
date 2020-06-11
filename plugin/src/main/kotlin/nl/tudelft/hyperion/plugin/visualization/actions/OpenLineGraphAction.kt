package nl.tudelft.hyperion.plugin.visualization.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
import git4idea.GitUtil
import nl.tudelft.hyperion.plugin.git.GitLineTracker
import nl.tudelft.hyperion.plugin.git.GitVersionResolver
import nl.tudelft.hyperion.plugin.git.OriginBlameReadResult
import nl.tudelft.hyperion.plugin.settings.HyperionSettings
import nl.tudelft.hyperion.plugin.util.getPsiPosition
import nl.tudelft.hyperion.plugin.visualization.VisToolWindowFactory
import nl.tudelft.hyperion.plugin.visualization.errorDialog

class OpenLineGraphAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val currentProject = e.getData(CommonDataKeys.PROJECT)
            ?: kotlin.run {
                errorDialog { "Current open project does not exist" }
                return@actionPerformed
            }
        val currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
            ?: kotlin.run {
                errorDialog { "Current open file is not linked to a project" }
                return@actionPerformed
            }
        val currentPSI = e.getData(CommonDataKeys.PSI_ELEMENT)
            ?: errorDialog { "Action was triggered outside of PSI context" }

        val originInfo = getLineOriginInfo(currentPSI, currentProject, currentFile)

        if (!currentFile.path.startsWith(currentProject.basePath!!)) {
            errorDialog { "file $currentFile is not in project ${currentProject.name}" }
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
            ?.show {
                VisToolWindowFactory.histogramTab.updateAllSettings()
                VisToolWindowFactory.histogramTab.queryAndUpdate()
                VisToolWindowFactory.histogramTab.root.repaint()
            }
    }

    private fun getLineOriginInfo(
        currentPSI: Any,
        currentProject: Project,
        currentFile: VirtualFile
    ): OriginBlameReadResult? {
        val lineInfo = getPsiPosition(currentPSI as PsiElement)
        return GitLineTracker.runOriginBlame(
            currentProject,
            GitUtil.getRepositoryForFile(currentProject, currentFile),
            currentFile,
            GitVersionResolver.getCurrentOriginCommit(currentProject)
                ?: kotlin.run {
                    errorDialog { "Current branch does not have an origin" }
                    return null
                },
            lineInfo.lineNumber
        )
    }
}