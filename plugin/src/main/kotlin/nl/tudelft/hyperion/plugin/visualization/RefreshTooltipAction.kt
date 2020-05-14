package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

class RefreshTooltipAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val navigatable = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (project != null && navigatable?.virtualFile != null && editor != null) {
            TooltipInlayManager.refreshMetrics(project, navigatable.virtualFile, editor)
        }
    }
}