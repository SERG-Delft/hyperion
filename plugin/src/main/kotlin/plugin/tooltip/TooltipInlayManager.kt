package plugin.tooltip

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile


class TooltipInlayManager {
    init {

    }
    companion object {
        fun addLogTooltip(editor: Editor, file: VirtualFile) {
            val document: Document = FileDocumentManager.getInstance().getDocument(file) ?: return

            val offset = getContentStartOffset(document)

            val renderer = TooltipRenderer("1k")
            val inlay = editor.inlayModel.addInlineElement(offset, true, renderer)
            println(inlay)
            println(inlay?.gutterIconRenderer)
        }

        private fun getContentStartOffset(document: Document): Int {
            val startOffset = document.getLineStartOffset(0)
            val endOffset = document.getLineEndOffset(0)
            val line = document.getText(TextRange(startOffset, endOffset))

            return startOffset + line.indexOf(line.trim())
        }
    }


}