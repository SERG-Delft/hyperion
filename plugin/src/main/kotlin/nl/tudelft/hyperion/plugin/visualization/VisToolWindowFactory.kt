package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory

class VisToolWindowFactory : ToolWindowFactory {
    companion object {
        lateinit var histogramTab: VisWindow
        lateinit var codeListTab: CodeList

        lateinit var histogramContent: Content
        lateinit var codeListContent: Content
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()

        histogramTab = VisWindow()
        histogramContent = contentFactory.createContent(
            histogramTab.content,
            "Histogram",
            false
        )

        codeListTab = CodeList()
        codeListContent = contentFactory.createContent(
            codeListTab.content,
            "Related lines",
            false
        )

        toolWindow.contentManager.addContent(histogramContent)
        toolWindow.contentManager.addContent(codeListContent)
        toolWindow.contentManager.setSelectedContent(histogramContent)
    }
}
