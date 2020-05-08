package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.vfs.VirtualFile

class FileOpenedListener : FileEditorManagerListener {

    private var listeners : MutableMap<Editor, MutableMap<Document, DocumentTooltipListener>> = mutableMapOf()
    private var documentLogInfos: MutableMap<Document, MutableSet<LogInfo>> = mutableMapOf()
    init {
        val bus = ApplicationManager.getApplication().messageBus
        val connection = bus.connect()
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
    }


    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return
        for (editor: FileEditor in source.getEditors(file)) {
            if (editor is TextEditor) {
                if (listeners[editor.editor] == null) listeners[editor.editor] = mutableMapOf()

                if (!listeners[editor.editor]!!.contains(document)) {
                    var logInfos: MutableSet<LogInfo>
                    if (documentLogInfos.contains(document)) {
                        logInfos = documentLogInfos[document]!!
                    } else {
                        // TODO: Obtain all logs for related file here and add to `logInfos`
                        logInfos = mutableSetOf(LogInfo(document, 0), LogInfo(document, 1))
                        documentLogInfos[document] = logInfos
                    }



                    val tooltips: MutableSet<Pair<LogInfo, Inlay<TooltipRenderer>>> = mutableSetOf()

                    for (logInfo in logInfos) {
                        tooltips.add(Pair(logInfo, TooltipInlayManager.addLogTooltip(editor.editor, logInfo)!!))
                    }
                    val listener = DocumentTooltipListener(tooltips)
                    document.addDocumentListener(listener)
                    listeners[editor.editor]?.set(document, listener)
                }
            }
        }


    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        val document: Document = FileDocumentManager.getInstance().getDocument(file) ?: return

        val stillOpenEditors = source.getAllEditors(file)
        val it = listeners.iterator()
        while (it.hasNext()) {
            val documentListeners = it.next()
            // If no listener exist for the document in question we move on to the next editor.
            if (!documentListeners.value.containsKey(document)) continue

            val selectedEditor: Editor = documentListeners.key
            if (!isStillOpen(selectedEditor, stillOpenEditors) && !listeners[selectedEditor].isNullOrEmpty()) {

                // Get DocumentTooltipListener and dispose it.
                listeners[selectedEditor]!!.remove(document)?.dispose()
                if (listeners[selectedEditor]!!.isEmpty()) {
                    // If there are no more listeners for the selected editor we remove it from the map.
                    it.remove()
                }
            }

        }

        super.fileClosed(source, file)
    }

    private fun isStillOpen(editor: Editor, stillOpenEditors: Array<FileEditor>): Boolean {
        for (openEditor in stillOpenEditors) {
            if (openEditor is TextEditor && openEditor.editor == editor) {
                return true
            }
        }
        return false
    }
}