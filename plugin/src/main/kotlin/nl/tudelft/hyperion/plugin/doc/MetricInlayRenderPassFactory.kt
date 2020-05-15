package nl.tudelft.hyperion.plugin.doc

import com.intellij.codeHighlighting.EditorBoundHighlightingPass
import com.intellij.codeHighlighting.Pass
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiModificationTracker
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.plugin.connection.ApiRequestor
import nl.tudelft.hyperion.plugin.metric.MetricsResult

val MODIFICATION_STAMP = Key.create<Long>("hyperion.render.stamp")
val ENABLED = Key.create<Boolean>("hyperion.render.enabled")

class MetricInlayRenderPassFactory : TextEditorHighlightingPassFactoryRegistrar, TextEditorHighlightingPassFactory,
    DumbAware {
    override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
        registrar.registerTextEditorHighlightingPass(
            this,
            TextEditorHighlightingPassRegistrar.Anchor.AFTER,
            Pass.POPUP_HINTS,
            false,
            false
        )
    }

    override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
        val current = PsiModificationTracker.SERVICE.getInstance(file.project).modificationCount
        val existing = editor.getUserData(MODIFICATION_STAMP)

        if (editor.project == null || existing != null && existing == current) {
            // No changes
            return null
        }

        val valid = metricsStillValid(editor)

        if (valid) {
            // lines only moved
            return null
        }

        return MetricInlayRenderPass(
            editor,
            file
        )
    }

    class MetricInlayRenderPass(editor: Editor, file: PsiFile) : EditorBoundHighlightingPass(
        editor, file,
        false
    ), DumbAware {
        var metrics: MetricsResult = MetricsResult(mapOf())

        override fun doCollectInformation(progress: ProgressIndicator) {
            progress.text = "Loading metrics..."

            runBlocking {
                delay(1000)

                val root = ProjectFileIndex.SERVICE.getInstance(myProject).getContentRootForFile(myFile.virtualFile)
                val filePath = root?.let { VfsUtilCore.getRelativePath(myFile.virtualFile, it) } ?: return@runBlocking

                metrics = ApiRequestor.getMetricForFile(filePath)
            }
        }

        override fun doApplyInformationToEditor() {
            setMetricsToEditor(myEditor, metrics)

            val newStamp = PsiModificationTracker.SERVICE.getInstance(myProject).modificationCount
            myEditor.putUserData(MODIFICATION_STAMP, newStamp)
        }
    }
}
