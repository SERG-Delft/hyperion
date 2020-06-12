package nl.tudelft.hyperion.plugin.visualization

import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Graphics
import java.awt.Rectangle

/**
 * Class that represents the Renderer for the [MetricInlayItem].
 * It is a slightly modified [HintRenderer] that also keeps track of a [RangeHighlighter] to
 * keep track of the start of the line (excluding tabs and spaces).
 */
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
