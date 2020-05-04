package plugin.tooltip

import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.GutterIconRenderer

class TooltipRenderer(text: String?, offset: Int) : AdvancedBlockRenderer(text, offset) {
    override fun calcGutterIconRenderer(inlay: Inlay<*>): GutterIconRenderer? {
        println("calcGutterIconRenderer")
        return GutterTooltipRenderer(inlay,0, "hi")
    }

    override fun getContextMenuGroupId(inlay: Inlay<*>): String? {
        return "HyperionTooltip"
    }
}