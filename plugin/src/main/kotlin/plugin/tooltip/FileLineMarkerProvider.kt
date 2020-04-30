package plugin.tooltip

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.util.ui.ImageUtil.createImage
import java.awt.Color
import java.awt.image.BufferedImage
import javax.swing.ImageIcon

class FileLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<PsiElement>>) {
        val psiFile = element.containingFile
        val document: Document = PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile) ?: return


        if (document.getLineNumber(element.textOffset) == 0) {
            val image = createImage(60, 60, BufferedImage.TYPE_INT_RGB)
            val graphics = image.createGraphics()

            graphics.paint = Color(94, 182, 52)
            graphics.fillRect(0, 0, image.width, image.height)

            val imageIcon = ImageIcon(image)

            val builder: NavigationGutterIconBuilder<PsiElement> = NavigationGutterIconBuilder.create(imageIcon)
                .setTarget(element).setTooltipTitle("Title").setTooltipText("Hey this is some cool description for you")
            result.add(builder.createLineMarkerInfo(element))
        }


    }
}