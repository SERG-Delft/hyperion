package plugin.tooltip

import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.util.ui.ImageUtil
import java.awt.Color
import java.awt.image.BufferedImage
import javax.swing.Icon
import javax.swing.ImageIcon

class GutterTooltipRenderer(val line: Int, val file: String) : GutterIconRenderer() {
    override fun hashCode(): Int {
        val result = (line.xor(line ushr 32))
        return 31 * result + file.hashCode()
    }

    override fun getIcon(): Icon {
        val image = ImageUtil.createImage(13, 13, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()

        graphics.paint = Color(94, 182, 52)
        graphics.fillRect(0, 0, image.width, image.height)

        return ImageIcon(image)
    }

    override fun equals(other: Any?): Boolean {
        if (other is GutterTooltipRenderer) {
            return line == other.line && file == other.file
        }
        return false
    }
}