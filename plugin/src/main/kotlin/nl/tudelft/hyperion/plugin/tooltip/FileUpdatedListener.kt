package nl.tudelft.hyperion.plugin.tooltip

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class FileUpdatedListener : BulkFileListener, FileListener() {
    init {
        val bus = ApplicationManager.getApplication().getMessageBus()
        val connection = bus.connect()
        connection.subscribe(VirtualFileManager.VFS_CHANGES, this)
    }

    override fun after(events: MutableList<out VFileEvent>) {
        for (event in events) {
            if (event is VFileContentChangeEvent) {
                val changeEvent: VFileContentChangeEvent = event
                println("Change event")

                val virtualFile: VirtualFile = changeEvent.file
                if (!virtualFile.isValid) continue
                println("Filename is: " + virtualFile.name)

                TODO("Following code should be changed to actually obtain project")
                val project: Project? = ProjectManager.getInstanceIfCreated()?.loadAndOpenProject(virtualFile.path)
                if (project != null) {
                    update(project, virtualFile)
                }

            }
        }
    }


}