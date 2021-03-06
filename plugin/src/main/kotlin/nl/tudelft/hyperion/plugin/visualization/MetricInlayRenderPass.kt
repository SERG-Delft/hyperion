package nl.tudelft.hyperion.plugin.visualization

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
import nl.tudelft.hyperion.plugin.metric.FileMetrics
import nl.tudelft.hyperion.plugin.metric.ResolvedFileMetrics
import nl.tudelft.hyperion.plugin.util.KeyedProperty
import nl.tudelft.hyperion.plugin.visualization.MetricInlayRenderPassFactory.Companion.modificationStamp

class MetricInlayRenderPass(editor: Editor, file: PsiFile) : EditorBoundHighlightingPass(
    editor, file,
    false
) {
    /**
     * This method is called by the IDE when a file needs to be highlighted.
     * We thus handle obtaining and (re)drawing the metrics here.
     */
    override fun doCollectInformation(progress: ProgressIndicator) {
        val metrics = getOrComputeMetrics()
        val resolved = myEditor.resolvedMetrics
        val items = myEditor.items ?: listOf()

        // If we don't have a resolved version or if any of our inlays
        // no longer has an anchor point, run a re-resolve from the file.
        if (myEditor.drawDisabled != true && (resolved == null || items.any { !it.isValid })) {
            myEditor.needsRedraw = true
            myEditor.resolvedMetrics = ResolvedFileMetrics.resolve(metrics, myProject, myFile.virtualFile)
        }
    }

    override fun doApplyInformationToEditor() {
        val positionKeeper = EditorScrollingPositionKeeper(myEditor)
        positionKeeper.savePosition()

        applyInformation()

        positionKeeper.restorePosition(false)
        MetricInlayRenderPassFactory.putCurrentModificationStamp(
            myEditor,
            myFile
        )
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
                createInlayForLine(
                    myEditor,
                    it.key,
                    it.value
                )
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
            requestFileMetrics()
        }
        myEditor.fileMetrics = result

        return result
    }

    /**
     * Uses the APIRequestor to request Metrics for the current file and handles it accordingly.
     * If the connection failed the exception is caught here and handled such that new requests won't be made
     * unless the file is reopened (or the refresh metrics action is used).
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun requestFileMetrics(): FileMetrics {
        val root = ProjectFileIndex.SERVICE.getInstance(myProject).getContentRootForFile(myFile.virtualFile)
        val filePath = root?.let { VfsUtilCore.getRelativePath(myFile.virtualFile, it) }

        return if (filePath != null) {
            try {
                APIRequestor.getMetricForFile(filePath, myProject)
            } catch (e: Exception) {
                // Catch BindException or ConnectException indicating the http request failed.
                myEditor.drawDisabled = true
                FileMetrics(mapOf())
            }
        } else {
            FileMetrics(mapOf())
        }
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

        @JvmStatic
        private val DRAW_DISABLED = Key.create<Boolean>("hyperion.disabled")

        var Editor.fileMetrics by KeyedProperty(FILE_METRICS)
        var Editor.resolvedMetrics by KeyedProperty(RESOLVED_METRICS)
        var Editor.items by KeyedProperty(ITEMS)
        var Editor.needsRedraw by KeyedProperty(NEEDS_REDRAW)
        var Editor.drawDisabled by KeyedProperty(DRAW_DISABLED)

        /**
         * This method is called by the [RefreshTooltipAction] to refresh all metrics for the current file.
         */
        fun forceRefresh(editor: Editor, file: PsiFile) {
            editor.resolvedMetrics = null
            editor.fileMetrics = null
            editor.modificationStamp = null
            editor.drawDisabled = false
            DaemonCodeAnalyzer.getInstance(editor.project).restart(file)
        }
    }
}
