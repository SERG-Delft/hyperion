package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.first
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.plugin.connection.ApiRequestor

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
                    GlobalScope.launch {
                        val logInfos: MutableSet<LogInfo> = getLogInfos(document, source, file)
                        ApplicationManager.getApplication().invokeAndWait({
                            placeMetrics(logInfos, editor, document)
                        }, ModalityState.NON_MODAL)

                    }
                }
            }
        }


    }

    private suspend fun getLogInfos(document: Document, source: FileEditorManager, file: VirtualFile): MutableSet<LogInfo> {
        val logInfos: MutableSet<LogInfo>
        if (documentLogInfos.contains(document)) {
            logInfos = documentLogInfos[document]!!
        } else {
            // TODO: Obtain all logs for related file here and add to `logInfos`
            logInfos = getMetrics(source.project, file, document)

            documentLogInfos[document] = logInfos
        }
        return logInfos
    }

    private fun placeMetrics(logInfos: MutableSet<LogInfo>, editor: TextEditor, document: Document) {
        val tooltips: MutableSet<Pair<LogInfo, Inlay<TooltipRenderer>>> = mutableSetOf()

        for (logInfo in logInfos) {
            tooltips.add(Pair(logInfo, TooltipInlayManager.addLogTooltip(editor.editor, logInfo)!!))
        }
        val listener = DocumentTooltipListener(tooltips)
        document.addDocumentListener(listener)
        listeners[editor.editor]?.set(document, listener)
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

    private suspend fun getMetrics(project: Project, file: VirtualFile, document: Document): MutableSet<LogInfo> {
        val root = ProjectFileIndex.SERVICE.getInstance(project).getContentRootForFile(file) ?: return mutableSetOf()
        val filePath = VfsUtilCore.getRelativePath(file, root) ?: return mutableSetOf()


        // TODO: Obtain all metrics for intervals for correct version
        val metricsResults = ApiRequestor.getMetricForFile(filePath)
        val logInfos: MutableSet<LogInfo> = mutableSetOf()
        for (metric in metricsResults.first().versions.first().value) {
            logInfos.add(LogInfo(document, metric.metric.line - 1))
        }

        return logInfos
    }
}