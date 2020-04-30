package plugin.tooltip

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile

class FileOpenedListener() : FileEditorManagerListener, FileListener() {
    init {
        val bus = ApplicationManager.getApplication().getMessageBus()
        val connection = bus.connect()
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
    }


    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        update(source.project, file)
    }
}