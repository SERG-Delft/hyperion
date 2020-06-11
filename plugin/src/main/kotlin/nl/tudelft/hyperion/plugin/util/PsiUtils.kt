package nl.tudelft.hyperion.plugin.util

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement

data class LineInfo(val lineNumber: Int, val textOffset: Int)

fun getPsiPosition(element: PsiElement): LineInfo {
    val containingFile = element.containingFile
    val project = containingFile.project
    val psiDocumentManager = PsiDocumentManager.getInstance(project)
    val document = psiDocumentManager.getDocument(containingFile)!!

    val textOffset = element.textOffset
    return LineInfo(lineNumber = document.getLineNumber(textOffset), textOffset = textOffset)
}