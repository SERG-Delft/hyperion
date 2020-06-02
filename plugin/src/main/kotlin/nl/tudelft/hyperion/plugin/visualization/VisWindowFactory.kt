package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class VisWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = VisWindow()
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(
            myToolWindow.content,
            "",
            false
        )
        toolWindow.contentManager.addContent(content)
    }
}
