package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import nl.tudelft.hyperion.plugin.git.GitLineTracker

class RefreshTooltipAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        /*
        val project = e.project
        val navigatable = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (project != null && navigatable?.virtualFile != null && editor != null) {
            TooltipInlayManager.refreshMetrics(project, navigatable.virtualFile, editor)
        }*/
        val navigatable = e.getData(CommonDataKeys.PSI_FILE)!!

        ProgressManager.getInstance().run(object : Backgroundable(e.project!!, "Resolving Log Locations") {
            override fun run(indicator: ProgressIndicator) {
                println(
                    "Line 11 is now line " +
                        GitLineTracker.resolveCurrentLine(
                            e.project!!, navigatable.virtualFile,
                            "a8c21c597f39502f85090f6abf795e716ef336e2", 11
                        )
                )
            }
        })
    }
}
