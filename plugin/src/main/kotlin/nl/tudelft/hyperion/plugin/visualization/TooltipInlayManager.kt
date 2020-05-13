package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay


class TooltipInlayManager {
    companion object {
        fun addLogTooltip(editor: Editor, logInfo: LogInfo): Inlay<TooltipRenderer>? {
            return editor.inlayModel.addBlockElement(logInfo.calculateLineOffset(),
                    false, true, 1, TooltipRenderer(logInfo.lineMetrics.getText(), logInfo))
        }

    }


}