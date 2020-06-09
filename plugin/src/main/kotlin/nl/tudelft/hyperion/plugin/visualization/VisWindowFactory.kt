package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class VisWindowFactory : ToolWindowFactory {
    companion object {
        lateinit var histogramTab: VisWindow
        lateinit var codeListTab: CodeList
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()

        histogramTab = VisWindow()
        val histogramContent = contentFactory.createContent(
            histogramTab.content,
            "Histogram",
            false
        )

        codeListTab = CodeList()
        val codeListContent = contentFactory.createContent(
            codeListTab.content,
            "Related lines",
            false
        )

        toolWindow.contentManager.addContent(histogramContent)
        toolWindow.contentManager.addContent(codeListContent)
        toolWindow.contentManager.setSelectedContent(histogramContent)
    }
}
