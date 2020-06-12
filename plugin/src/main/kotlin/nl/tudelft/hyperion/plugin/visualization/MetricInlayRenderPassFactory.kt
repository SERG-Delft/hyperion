package nl.tudelft.hyperion.plugin.visualization

import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiFile
import nl.tudelft.hyperion.plugin.util.KeyedProperty

/**
 * Class that handles the creation of HighlightingPasses [MetricInlayRenderPass] for given file and editor.
 */
class MetricInlayRenderPassFactory : TextEditorHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar {
    override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
        registrar.registerTextEditorHighlightingPass(this, null, null, false, -1)
    }

    override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
        if (editor.isOneLineMode) return null

        // Ignore if the file hasn't changed.
        val savedStamp = editor.modificationStamp
        val currentStamp = getCurrentModificationStamp(
            file
        )
        if (savedStamp != null && savedStamp == currentStamp) return null

        return MetricInlayRenderPass(editor, file)
    }

    companion object {
        @JvmStatic
        private val PSI_MODIFICATION_STAMP = Key.create<Long>("hyperion.inlay.psi.modification.stamp")

        var Editor.modificationStamp by KeyedProperty(PSI_MODIFICATION_STAMP)

        fun putCurrentModificationStamp(editor: Editor, file: PsiFile) {
            editor.modificationStamp = getCurrentModificationStamp(
                file
            )
        }

        private fun getCurrentModificationStamp(file: PsiFile): Long {
            return file.manager.modificationTracker.modificationCount
        }
    }
}
