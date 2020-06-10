package nl.tudelft.hyperion.plugin.visualization.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import nl.tudelft.hyperion.plugin.doc.MetricInlayRenderPass

class RefreshTooltipAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        if (editor != null && psiFile != null) {
            MetricInlayRenderPass.forceRefresh(editor, psiFile)
        }
    }
}
