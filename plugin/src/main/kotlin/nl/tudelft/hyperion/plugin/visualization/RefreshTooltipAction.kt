package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys

/**
 * Class that represents the action in the IDE that can be fired by the user.
 * It resides under the "Tools" menu and has the hotkey CTRL+ALT+H, F
 */
class RefreshTooltipAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        if (editor != null && psiFile != null) {
            MetricInlayRenderPass.forceRefresh(editor, psiFile)
        }
    }
}
