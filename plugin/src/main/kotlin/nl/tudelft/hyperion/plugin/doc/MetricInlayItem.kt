package nl.tudelft.hyperion.plugin.doc

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.jetbrains.rd.util.first
import nl.tudelft.hyperion.plugin.metric.MetricsResult
import nl.tudelft.hyperion.plugin.visualization.LogInfo
import java.awt.Color

val ITEMS = Key.create<MutableList<MetricInlayItem>>("hyperion.render.items")

class MetricInlayItem(
    val inlay: Inlay<MetricTooltipRenderer>,
    val highlighter: RangeHighlighter
) {
    val isValid
        get() = highlighter.isValid

    fun remove() {
        if (highlighter.isValid) {
            highlighter.dispose()
        }

        if (inlay.isValid) {
            Disposer.dispose(inlay)
        }
    }
}

fun metricsStillValid(editor: Editor): Boolean {
    val existing = editor.getUserData(ITEMS) ?: mutableListOf()
    return !existing.isEmpty() && existing.all { it.isValid }
}

fun setMetricsToEditor(editor: Editor, items: MetricsResult) {
    val existing = editor.getUserData(ITEMS) ?: mutableListOf()
    editor.putUserData(ITEMS, existing)

    keepScrollingPositionWhile(editor) {
        for (item in existing) item.remove()
        existing.clear()

        if (items.versions.isEmpty()) return@keepScrollingPositionWhile

        // For every metric in this file...
        for (lineMetric in items.versions.first().value) {
            val info = LogInfo(editor.document, lineMetric)

            val highlighter = editor.markupModel.addLineHighlighter(
                lineMetric.getLine(),
                HighlighterLayer.HYPERLINK,
                TextAttributes().also {
                    it.backgroundColor = Color.GREEN
                }
            )

            // highlighter.isGreedyToRight = true

            val inlay = editor.inlayModel.addBlockElement(
                info.calculateLineOffset(),
                false,
                true,
                1,
                MetricTooltipRenderer(info.lineMetrics.getText(), highlighter)
            )!!

            existing.add(MetricInlayItem(inlay, highlighter))
        }
    }
}

fun keepScrollingPositionWhile(editor: Editor, block: () -> Unit) {
    val pos = EditorScrollingPositionKeeper(editor)
    pos.savePosition()
    block()
    pos.restorePosition(false)
}
