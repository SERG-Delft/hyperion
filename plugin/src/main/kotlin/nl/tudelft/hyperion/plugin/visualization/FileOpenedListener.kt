package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.vfs.VirtualFile

class FileOpenedListener : FileEditorManagerListener {

    private var listeners : MutableMap<Document, DocumentTooltipListener> = mutableMapOf()

    init {
        val bus = ApplicationManager.getApplication().messageBus
        val connection = bus.connect()
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
    }


    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val document = FileDocumentManager.getInstance().getDocument(file)
        if (document != null && !listeners.contains(document) ) {
            val logInfos: MutableSet<LogInfo> = mutableSetOf(LogInfo(document, 0), LogInfo(document, 1))
            // TODO: Obtain all logs for related file here and add to `logInfos`

            val tooltips: MutableSet<Pair<LogInfo, Inlay<TooltipRenderer>>> = mutableSetOf()

            for (editor: FileEditor in source.getEditors(file)) {
                if (editor is TextEditor) {
                    for (logInfo in logInfos) {
                        tooltips.add(Pair(logInfo, TooltipInlayManager.addLogTooltip(editor.editor, logInfo)!!))
                    }
                }
            }
            println(logInfos)
            val listener = DocumentTooltipListener(tooltips)
            document.addDocumentListener(listener)
            listeners[document] = listener
        }


    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        // In case the file is still opened somewhere else we do nothing.
        if (source.isFileOpen(file)) return

        // Get DocumentTooltipListener and dispose it.
        listeners.remove(FileDocumentManager.getInstance().getDocument(file))?.dispose()


        super.fileClosed(source, file)
    }
}