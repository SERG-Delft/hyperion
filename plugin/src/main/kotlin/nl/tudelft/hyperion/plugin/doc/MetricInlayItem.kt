package nl.tudelft.hyperion.plugin.doc

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import org.joda.time.Period
import org.joda.time.format.PeriodFormatterBuilder

class MetricInlayItem(
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

private val formatter = PeriodFormatterBuilder()
    .appendWeeks().appendSuffix(" w").appendSeparator(" ")
    .appendDays().appendSuffix(" d").appendSeparator(" ")
    .appendHours().appendSuffix(" h").appendSeparator(" ")
    .appendMinutes().appendSuffix(" min").appendSeparator(" ")
    .appendSeconds().appendSuffix(" s").appendSeparator(" ")
    .toFormatter()

/**
 * Converts an interval: count map into a string that represents this datapoint
 * for use in the block inlay label.
 */
fun countsToLabel(counts: Map<Int, Int>): String {
    return counts.toList().sortedBy { it.first }.map {
        val prettyTime = formatter.print(Period(it.first * 1000L).normalizedStandard())
        "[${it.second} last $prettyTime]"
    }.joinToString(" ")
}

/**
 * Creates and adds a new inlay with the specified trigger counts at the specified
 * line in the specified editor.
 */
fun createInlayForLine(editor: Editor, line: Int, counts: Map<Int, Int>): MetricInlayItem {
    val inlayOffset = calculateInlayOffset(editor, line)

    // Create a highlighter attached to this first element.
    val highlighter = editor.markupModel.addRangeHighlighter(
        inlayOffset,
        inlayOffset + 1,
        HighlighterLayer.HYPERLINK,
        TextAttributes(),
        HighlighterTargetArea.EXACT_RANGE
    )

    // And attach an inlay to that highlighter
    val inlay = createInlay(editor, inlayOffset, countsToLabel(counts), highlighter)

    return MetricInlayItem(inlay, highlighter)
}

private fun calculateInlayOffset(editor: Editor, line: Int): Int {
    // Figure out the first character on the line.
    val startOffset = editor.document.getLineStartOffset(line - 1)
    val endOffset = editor.document.getLineEndOffset(line - 1)
    val lineText = editor.document.getText(TextRange(startOffset, endOffset))
    val inlayOffset = startOffset + lineText.indexOf(lineText.trim())
    return inlayOffset
}

private fun createInlay(editor: Editor, offset: Int, text: String?, highlighter: RangeHighlighter):
        Inlay<MetricTooltipRenderer> {
    return editor.inlayModel.addBlockElement(
            offset,
            false,
            true,
            1,
            MetricTooltipRenderer(text, highlighter)
    )!!
}

/**
 * Potentially recreates the inlay that belongs to the attached item,
 * depending on whether or not is is placed on the correct position.
 *
 * It is expected that the item is valid when this function is called.
 */
fun updateMetricInlayItem(editor: Editor, item: MetricInlayItem): MetricInlayItem {
    if (isFullyValid(item)) return item

    // Recreate the inlay at the current highlighter offset. We cannot
    // move it, as intellij does not support the movement of inlays.
    item.inlay = createInlay(editor, item.highlighter.startOffset, item.inlay.renderer.text, item.highlighter)

    return item
}

private fun isFullyValid(item: MetricInlayItem): Boolean {
    if (!item.isValid) {
        throw IllegalStateException("All items should be valid at this stage")
    }

    // Nothing to do.
    if (item.isProperlyPlaced) {
        return true
    }

    if (item.inlay.isValid) {
        Disposer.dispose(item.inlay)
    }
    return false
}
