package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener

class DocumentTooltipListener(private var tooltips : MutableSet<Pair<LogInfo, Inlay<TooltipRenderer>>>)
    : DocumentListener, Disposable {

    private var document: Document? = if (tooltips.isNotEmpty()) {
        tooltips.first().first.document
    } else {
        null
    }


    override fun dispose() {
        this.document?.removeDocumentListener(this)
    }

    override fun documentChanged(event: DocumentEvent) {
        updateTooltips(event)
    }


    private fun updateTooltips(event: DocumentEvent) {
        val tooltipsToAdd: MutableSet<Pair<LogInfo, Inlay<TooltipRenderer>>> = mutableSetOf()
        val it = tooltips.iterator()
        while (it.hasNext()) {
            val tooltip = it.next()

            if (tooltip.second.offset >= event.offset && tooltip.second.offset < event.offset + event.oldLength + event.newLength) {

                // Dispose tooltip inlay.
                val editor = tooltip.second.editor
                tooltip.second.dispose()

                // TODO: Update line number using git.
                // Re-add the tooltip inlay.
                it.remove()
                tooltipsToAdd.add(tooltip.copy(second = TooltipInlayManager.addLogTooltip(editor, tooltip.first)!!))
            }
        }
        tooltips.addAll(tooltipsToAdd)
    }
}