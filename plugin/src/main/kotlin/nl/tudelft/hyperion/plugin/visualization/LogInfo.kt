package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import nl.tudelft.hyperion.plugin.metric.LineMetrics

class LogInfo(val document: Document, val lineMetrics: LineMetrics) {

    fun calculateLineOffset(): Int {
        val startOffset = document.getLineStartOffset(lineMetrics.line)
        val endOffset = document.getLineEndOffset(lineMetrics.line)
        val line = document.getText(TextRange(startOffset, endOffset))

        return startOffset + line.indexOf(line.trim())
    }
}
