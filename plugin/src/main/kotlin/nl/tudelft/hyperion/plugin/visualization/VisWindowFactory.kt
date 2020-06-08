package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class VisWindowFactory : ToolWindowFactory {
    companion object {
        lateinit var histogramTab: VisWindow
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        histogramTab = VisWindow()
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(
            histogramTab.content,
            "",
            false
        )
        toolWindow.contentManager.addContent(content)
    }
}
