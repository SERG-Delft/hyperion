package plugin.tooltip

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile


class TooltipInlayManager {
    companion object {
        fun addLogTooltip(editor: Editor, file: VirtualFile) {
            val document: Document = FileDocumentManager.getInstance().getDocument(file) ?: return

            val offset = getContentStartOffset(document)

            val renderer = TooltipRenderer("1,029 last 5 min; 200,199 last week", offset)
            val inlay = editor.inlayModel.addBlockElement(offset, false, true, 1, renderer)
            inlay!!.update()
            println(inlay::class.java)
            println(inlay)
            println(inlay?.gutterIconRenderer)
        }

        private fun getContentStartOffset(document: Document): Int {
            val startOffset = document.getLineStartOffset(18)
            val endOffset = document.getLineEndOffset(18)
            val line = document.getText(TextRange(startOffset, endOffset))
            println(line)

            return startOffset + line.indexOf(line.trim())
        }
    }
}