package plugin.tooltip

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.vfs.VirtualFile

class FileOpenedListener() : FileEditorManagerListener, FileListener() {
    init {
        val bus = ApplicationManager.getApplication().getMessageBus()
        val connection = bus.connect()
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
    }


    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        update(source.project, file)

        for (editor: FileEditor in source.getEditors(file)) {
            if (editor is TextEditor) {
                TooltipInlayManager.addLogTooltip(editor.editor, file)
            }
        }
    }
}