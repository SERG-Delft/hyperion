package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange

class LogInfo(val document: Document, var line: Int) {
    private var lineOffset: Int

    init {
        lineOffset = calculateLineOffset()
    }

    fun calculateLineOffset(): Int {
        val startOffset = document.getLineStartOffset(line)
        val endOffset = document.getLineEndOffset(line)
        val line = document.getText(TextRange(startOffset, endOffset))

        return startOffset + line.indexOf(line.trim())
    }
}