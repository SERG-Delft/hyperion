package nl.tudelft.hyperion.plugin.doc

import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Graphics
import java.awt.Rectangle

class MetricTooltipRenderer(
    text: String?,
    private val highlighter: RangeHighlighter
) : HintRenderer(text) {
    private val offset: Int
        get() = highlighter.startOffset

    override fun paint(inlay: Inlay<*>, g: Graphics, r: Rectangle, textAttributes: TextAttributes) {
        val x = inlay.editor.offsetToXY(offset).x
        r.x = x

        super.paint(inlay, g, r, textAttributes)
    }
}
