package nl.tudelft.hyperion.plugin.doc

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import nl.tudelft.hyperion.plugin.metric.LineMetrics
import java.awt.Color

class MetricInlayItem(
    val metric: LineMetrics,
    var inlay: Inlay<MetricTooltipRenderer>,
    val highlighter: RangeHighlighter
) {
    val isValid
        get() = highlighter.isValid

    val isProperlyPlaced
        get() = inlay.offset == highlighter.startOffset

    fun remove() {
        if (highlighter.isValid) {
            highlighter.dispose()
        }

        if (inlay.isValid) {
            Disposer.dispose(inlay)
        }
    }
}

fun createInlayForMetric(editor: Editor, metric: LineMetrics): MetricInlayItem? {
    // TODO: resolve current location of line in file, return null if it doesn't exist
    return keepScrollingPositionWhile(editor) {
        val startOffset = editor.document.getLineStartOffset(metric.line)
        val endOffset = editor.document.getLineEndOffset(metric.line)
        val line = editor.document.getText(TextRange(startOffset, endOffset))
        val inlayOffset = startOffset + line.indexOf(line.trim())

        val highlighter = editor.markupModel.addRangeHighlighter(
            inlayOffset,
            inlayOffset + 1,
            HighlighterLayer.HYPERLINK,
            TextAttributes().also { it.backgroundColor = Color.RED },
            HighlighterTargetArea.EXACT_RANGE
        )

        val inlay = editor.inlayModel.addBlockElement(
            inlayOffset,
            false,
            true,
            1,
            MetricTooltipRenderer(metric.text, highlighter)
        )!!

        MetricInlayItem(metric, inlay, highlighter)
    }
}

// returns null if we cannot find a new location for the item
fun updateMetricInlayItem(editor: Editor, item: MetricInlayItem): MetricInlayItem? {
    if (item.isValid && item.isProperlyPlaced) return item

    if (item.isValid && !item.isProperlyPlaced) {
        if (item.inlay.isValid) {
            Disposer.dispose(item.inlay)
        }

        item.inlay = editor.inlayModel.addBlockElement(
            item.highlighter.startOffset,
            false,
            true,
            1,
            MetricTooltipRenderer(item.metric.text, item.highlighter)
        )!!

        return item
    }

    item.remove()
    return createInlayForMetric(editor, item.metric)
}

fun <T> keepScrollingPositionWhile(editor: Editor, block: () -> T): T {
    val pos = EditorScrollingPositionKeeper(editor)
    pos.savePosition()
    val res = block()
    pos.restorePosition(false)
    return res
}
