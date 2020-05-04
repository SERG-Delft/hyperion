package nl.tudelft.hyperion.plugin.tooltip

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.util.ui.ImageUtil
import java.awt.Color
import java.awt.image.BufferedImage
import javax.swing.ImageIcon

class LogExternalAnnotator : ExternalAnnotator<String, String>() {
    override fun collectInformation(file: PsiFile): String? {
        println("Collect")
        return "yo"
    }

    override fun doAnnotate(collectedInfo: String?): String? {
        println("Annotate")
        return "yo"
    }

    override fun apply(file: PsiFile, annotationResult: String?, holder: AnnotationHolder) {
        println("Apply")
        val offsets = file.text.foldIndexed(mutableListOf(0)) { i, offsets, c -> if (c == '\n') offsets += (i + 1); offsets }

        val image = ImageUtil.createImage(18, 18, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()

        graphics.paint = Color(94, 182, 52)
        graphics.fillRect(0, 0, image.width, image.height)

        val imageIcon = ImageIcon(image)

        holder.newAnnotation(HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING, "Test")
                .gutterIconRenderer(object : GutterIconRenderer() {
                    override fun hashCode() = imageIcon.hashCode()
                    override fun getIcon() = imageIcon
                    override fun equals(other: Any?) = other.hashCode() == hashCode()
                })
                .range(TextRange(offsets[5], offsets[6]))
                .highlightType(ProblemHighlightType.WARNING)
                .enforcedTextAttributes(TextAttributes().also { it.backgroundColor = Color.RED })
                .create()
    }
}