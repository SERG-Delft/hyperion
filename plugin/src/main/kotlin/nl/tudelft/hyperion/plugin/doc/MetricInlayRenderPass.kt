package nl.tudelft.hyperion.plugin.doc

import com.intellij.codeHighlighting.EditorBoundHighlightingPass
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.util.EditorScrollingPositionKeeper
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.plugin.connection.APIRequestor
import nl.tudelft.hyperion.plugin.doc.MetricInlayRenderPassFactory.Companion.modificationStamp
import nl.tudelft.hyperion.plugin.metric.FileMetrics
import nl.tudelft.hyperion.plugin.metric.ResolvedFileMetrics
import nl.tudelft.hyperion.plugin.util.KeyedProperty

class MetricInlayRenderPass(editor: Editor, file: PsiFile) : EditorBoundHighlightingPass(
    editor, file,
    false
) {
    override fun doCollectInformation(progress: ProgressIndicator) {
        val metrics = getOrComputeMetrics()
        val resolved = myEditor.resolvedMetrics
        val items = myEditor.items ?: listOf()

        // If we don't have a resolved version or if any of our inlays
        // no longer has an anchor point, run a re-resolve from the file.
        if (resolved == null || items.any { !it.isValid }) {
            myEditor.needsRedraw = true
            myEditor.resolvedMetrics = ResolvedFileMetrics.resolve(metrics, myProject, myFile.virtualFile)
        }
    }

    override fun doApplyInformationToEditor() {
        val positionKeeper = EditorScrollingPositionKeeper(myEditor)
        positionKeeper.savePosition()

        applyInformation()

        positionKeeper.restorePosition(false)
        MetricInlayRenderPassFactory.putCurrentModificationStamp(myEditor, myFile)
    }

    /**
     * Applies the retrieved metrics to the editor through methods inside
     * [MetricInlayItem].
     */
    private fun applyInformation() {
        val resolved = myEditor.resolvedMetrics ?: return
        var items = myEditor.items ?: listOf()

        // Redraw if needed.
        if (myEditor.needsRedraw == true) {
            myEditor.needsRedraw = false

            items.forEach(MetricInlayItem::remove)

            items = resolved.lineSums.map {
                createInlayForLine(myEditor, it.key, it.value)
            }
        }

        // Reposition if needed.
        items = items.map {
            updateMetricInlayItem(myEditor, it)
        }

        // Update
        myEditor.items = items
    }

    /**
     * Retrieves the cached metrics for this editor, or fetches them
     * from the server synchronously if there's none cached.
     */
    private fun getOrComputeMetrics(): FileMetrics {
        val metrics = myEditor.fileMetrics
        if (metrics != null) return metrics

        val result = runBlocking {
            val root = ProjectFileIndex.SERVICE.getInstance(myProject).getContentRootForFile(myFile.virtualFile)
            val filePath = root?.let { VfsUtilCore.getRelativePath(myFile.virtualFile, it) }

            if (filePath != null) {
                APIRequestor.getMetricForFile(filePath, myProject)
            } else {
                FileMetrics(mapOf())
            }
        }
        myEditor.fileMetrics = result

        return result
    }

    companion object {
        @JvmStatic
        private val FILE_METRICS = Key.create<FileMetrics>("hyperion.metrics")

        @JvmStatic
        private val RESOLVED_METRICS = Key.create<ResolvedFileMetrics>("hyperion.metrics.resolved")

        @JvmStatic
        private val ITEMS = Key.create<List<MetricInlayItem>>("hyperion.items")

        @JvmStatic
        private val NEEDS_REDRAW = Key.create<Boolean>("hyperion.redraw")

        var Editor.fileMetrics by KeyedProperty(FILE_METRICS)
        var Editor.resolvedMetrics by KeyedProperty(RESOLVED_METRICS)
        var Editor.items by KeyedProperty(ITEMS)
        var Editor.needsRedraw by KeyedProperty(NEEDS_REDRAW)

        fun forceRefresh(editor: Editor, file: PsiFile) {
            editor.resolvedMetrics = null
            editor.fileMetrics = null
            editor.modificationStamp = null
            DaemonCodeAnalyzer.getInstance(editor.project).restart(file)
        }
    }
}
