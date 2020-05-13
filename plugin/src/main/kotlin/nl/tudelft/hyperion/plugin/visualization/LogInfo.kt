package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import nl.tudelft.hyperion.plugin.metric.LineMetrics

class LogInfo(val document: Document, val lineMetrics: LineMetrics) {

    fun calculateLineOffset(): Int {
        val startOffset = document.getLineStartOffset(lineMetrics.getLine())
        val endOffset = document.getLineEndOffset(lineMetrics.getLine())
        val line = document.getText(TextRange(startOffset, endOffset))

        return startOffset + line.indexOf(line.trim())
    }
}