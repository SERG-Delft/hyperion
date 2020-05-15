package nl.tudelft.hyperion.plugin.doc

import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import java.awt.Graphics
import java.awt.Rectangle

class MetricTooltipRenderer(
    text: String?,
    private val highlighter: RangeHighlighter
) : HintRenderer(text) {

    val offset: Int
        get() {
            val start = highlighter.startOffset
            val end = highlighter.endOffset
            val line = highlighter.document.getText(TextRange(start, end))

            return start + line.indexOf(line.trim())
        }

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val x = inlay.editor.offsetToXY(offset).x
        return x + super.calcWidthInPixels(inlay)
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, r: Rectangle, textAttributes: TextAttributes) {
        val x = inlay.editor.offsetToXY(offset).x
        r.x += x
        r.width -= x

        super.paint(inlay, g, r, textAttributes)
    }
}
