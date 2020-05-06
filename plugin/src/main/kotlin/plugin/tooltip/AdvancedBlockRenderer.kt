package plugin.tooltip

import com.intellij.codeInsight.daemon.impl.HintRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Graphics
import java.awt.Rectangle

open class AdvancedBlockRenderer(text: String?, val offset: Int) : HintRenderer(text) {
    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val x = inlay.editor.offsetToXY(offset).x
        return x + super.calcWidthInPixels(inlay)
    }

    override fun paint(inlay: Inlay<*>, g: Graphics, r: Rectangle, textAttributes: TextAttributes) {
        val x = inlay.editor.offsetToXY(offset).x
        r.x += x
        r.width -= x

        super.paint(inlay, g, r, textAttributes)
    }
}
