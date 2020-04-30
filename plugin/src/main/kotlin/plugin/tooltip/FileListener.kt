package plugin.tooltip

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint


open class FileListener {
    fun update(project: Project, file: VirtualFile) {
        val statusBar = WindowManager.getInstance().getStatusBar(project)
        JBPopupFactory.getInstance().createHtmlTextBalloonBuilder("Opened file: " + file.name
                + " with length " + file.length,
                MessageType.INFO, null).setFadeoutTime(5000).createBalloon().show(
                statusBar?.component?.let { RelativePoint.getCenterOf(it) },
                Balloon.Position.atRight
        )
    }

}