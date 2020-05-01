package plugin.tooltip

import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.GutterIconRenderer

class TooltipRenderer(text: String?) : HintRenderer(text) {

    override fun calcGutterIconRenderer(inlay: Inlay<*>): GutterIconRenderer? {
        return GutterTooltipRenderer(0, "hi")
    }
    override fun getContextMenuGroupId(inlay: Inlay<*>): String? {
        return "HyperionTooltip"
    }
}