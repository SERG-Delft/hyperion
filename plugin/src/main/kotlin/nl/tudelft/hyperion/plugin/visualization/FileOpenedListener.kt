package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.vfs.VirtualFile

class FileOpenedListener : FileEditorManagerListener {

    init {
        val bus = ApplicationManager.getApplication().messageBus
        val connection = bus.connect()
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
    }

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val document = FileDocumentManager.getInstance().getDocument(file) ?: return
        for (editor: FileEditor in source.getEditors(file)) {
            if (editor is TextEditor) {
                TooltipInlayManager.handleFileOpened(editor.editor, document, file, source.project)
            }
        }


    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        val document: Document = FileDocumentManager.getInstance().getDocument(file) ?: return

        val stillOpenEditors = source.getAllEditors(file)
        TooltipInlayManager.handleFileClosed(document, stillOpenEditors)

        super.fileClosed(source, file)
    }

}
