package nl.tudelft.hyperion.plugin.visualization

import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Graphics
import java.awt.Rectangle

class TooltipRenderer(text: String?, private val logInfo: LogInfo) : HintRenderer(text) {

    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val x = inlay.editor.offsetToXY(logInfo.calculateLineOffset()).x
        return x + super.calcWidthInPixels(inlay)
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, r: Rectangle, textAttributes: TextAttributes) {
        val x = inlay.editor.offsetToXY(logInfo.calculateLineOffset()).x
        r.x += x
        r.width -= x

        super.paint(inlay, g, r, textAttributes)
    }
}
