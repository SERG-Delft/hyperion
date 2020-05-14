package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rd.util.first
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.plugin.connection.ApiRequestor


object TooltipInlayManager {
    private var listeners : MutableMap<Editor, MutableMap<Document, DocumentTooltipListener>> = mutableMapOf()
    private var documentLogInfos: MutableMap<Document, MutableSet<LogInfo>> = mutableMapOf()


    fun addLogTooltip(editor: Editor, logInfo: LogInfo): Inlay<TooltipRenderer>? {
        return editor.inlayModel.addBlockElement(logInfo.calculateLineOffset(),
                false, true, 1, TooltipRenderer(logInfo.lineMetrics.getText(), logInfo))
    }

    fun refreshMetrics(project: Project, file: VirtualFile, editor: Editor) {
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return
        ApplicationManager.getApplication().invokeAndWait({
            handleFileClosed(document, emptyArray())
            handleFileOpened(editor, document, file, project)
        }, ModalityState.NON_MODAL)
    }

    fun handleFileOpened(editor: Editor, document: Document, file: VirtualFile, project: Project) {
        if (listeners[editor] == null) listeners[editor] = mutableMapOf()

        if (!listeners[editor]!!.contains(document)) {
            GlobalScope.launch {
                val logInfos: MutableSet<LogInfo> = getLogInfos(document, project, file)
                ApplicationManager.getApplication().invokeAndWait({
                    placeMetrics(logInfos, editor, document)
                }, ModalityState.NON_MODAL)

            }
        }
    }

    fun handleFileClosed(document: Document, stillOpenEditors: Array<FileEditor>) {
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
    }
    private fun isStillOpen(editor: Editor, stillOpenEditors: Array<FileEditor>): Boolean {
        for (openEditor in stillOpenEditors) {
            if (openEditor is TextEditor && openEditor.editor == editor) {
                return true
            }
        }
        return false
    }

    private fun placeMetrics(logInfos: MutableSet<LogInfo>, editor: Editor, document: Document) {
        val tooltips: MutableSet<Pair<LogInfo, Inlay<TooltipRenderer>>> = mutableSetOf()

        for (logInfo in logInfos) {
            tooltips.add(Pair(logInfo, addLogTooltip(editor, logInfo)!!))
        }
        val listener = DocumentTooltipListener(tooltips)
        document.addDocumentListener(listener)
        listeners[editor]?.set(document, listener)
    }

    private suspend fun getLogInfos(document: Document,
                                    project: Project, file: VirtualFile): MutableSet<LogInfo> {
        val logInfos: MutableSet<LogInfo>
        if (documentLogInfos.contains(document)) {
            logInfos = documentLogInfos[document]!!
        } else {
            // TODO: Obtain all logs for related file here and add to `logInfos`
            logInfos = getMetrics(project, file, document)

            documentLogInfos[document] = logInfos
        }
        return logInfos
    }

    private suspend fun getMetrics(project: Project, file: VirtualFile, document: Document): MutableSet<LogInfo> {
        val root = ProjectFileIndex.SERVICE.getInstance(project).getContentRootForFile(file)
        val filePath = root?.let { VfsUtilCore.getRelativePath(file, it) } ?: return mutableSetOf()


        // TODO: Obtain all metrics for intervals for correct version
        val metricsResults = ApiRequestor.getMetricForFile(filePath)
        val logInfos: MutableSet<LogInfo> = mutableSetOf()
        for (lineMetric in metricsResults.versions.first().value) {
            // Cycle through LineMetrics
            logInfos.add(LogInfo(document, lineMetric))
        }

        return logInfos
    }


}
