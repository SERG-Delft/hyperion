package nl.tudelft.hyperion.plugin.graphs

import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.geom.AffineTransform
import javax.swing.JPanel
import kotlin.math.PI
import kotlin.math.round

typealias Array2D<T> = Array<Array<T>>
typealias Index2D = Pair<Int, Int>

class InteractiveHistogram(
    initialVals: Array2D<Int>,
    private val xMargin: Int,
    private var startY: Int,
    private var endY: Int,
    var barSpacing: Int,
    var barColorScheme: Array<Color>,
    var labels: Array<String>,
    var timestamps: Array<String>
) : JPanel(true) {

    var boxes: Array2D<Box> = initialVals.map { it.map { Box.default() }.toTypedArray() }.toTypedArray()
    var boxCollisions: List<Index2D> = listOf()
    var isCurrentlyColored = false
    private val rotatedFont: Font

    /**
     * A 2D array of the histogram values.
     * Change array size of [boxes] if this value changes.
     */
    var vals = initialVals
        set(value) {
            field = value

            // Create array of uninitialized boxes
            boxes = vals.map { it.map { Box.default() }.toTypedArray() }.toTypedArray()
        }

    init {
        this.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent?) {
                boxCollisions = checkCollide(e?.x!!, e.y)

                // Only redraw if the cursor is hovering over any of the boxes
                if (boxCollisions.isNotEmpty()) {
                    // TODO: make it only repaint the rect of what needs to be redrawn
                    //  instead of the entire JPanel
                    this@InteractiveHistogram.repaint()
                    isCurrentlyColored = true
                } else if (isCurrentlyColored) {
                    this@InteractiveHistogram.repaint()
                    isCurrentlyColored = false
                }
            }
        })

        font = Font("Monospaced", Font.PLAIN, 10)

        val affineTransform = AffineTransform()
        affineTransform.rotate(-PI / 2, 0.0, 0.0)
        rotatedFont = font.deriveFont(affineTransform)
    }

    companion object {
        const val Y_LABEL = "count"
    }

    private fun calculateBoxes() {
        val histogramWidth = this.width - 2 * xMargin
        val barWidth = histogramWidth / vals.size

        val maxBarTotal = vals.map(Array<Int>::sum).max()
        val barHeightScale = (startY - endY) / maxBarTotal!!.toDouble()

        for (i in vals.indices) {
            var prevY = 0
            val leftMargin = xMargin + i * barWidth

            for (j in vals[i].indices) {
                val currentY = round(vals[i][j] * barHeightScale).toInt()

                boxes[i][j].startX = leftMargin + barSpacing / 2
                boxes[i][j].startY = startY - (prevY + currentY)
                boxes[i][j].width = barWidth - barSpacing
                boxes[i][j].height = currentY

                prevY += currentY
            }
        }
    }

    @SuppressWarnings("MagicNumber")
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        g.color = Color.GRAY
        g.drawLine(xMargin, startY, this.width - xMargin, startY)
        g.drawLine(xMargin, startY, xMargin, endY)

        val g2 = g as Graphics2D

        // Set anti aliasing
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g2.font = rotatedFont
        val yLabelLength = g.getFontMetrics(font).stringWidth(Y_LABEL)
        g2.drawString(Y_LABEL, xMargin - 5, endY + (startY - endY) / 2 + yLabelLength / 2)
        g2.font = font

        drawHistogram(g)
    }

    private fun drawHistogram(g: Graphics) {
        // TODO: only recalculate boxes on component show or component resize
        calculateBoxes()

        for ((i, bar) in boxes.withIndex()) {

            // Draw timestamps
            val firstBox = bar.first()
            val xLabelFontMetrics = g.getFontMetrics(font)
            g.color = Color.GRAY
            g.drawString(
                timestamps[i],
                firstBox.startX + firstBox.width - xLabelFontMetrics.stringWidth(timestamps[i]) / 2,
                startY + xLabelFontMetrics.height
            )

            // Draw bars
            for ((j, box) in bar.withIndex()) {
                g.color = barColorScheme[j]
                g.fillRect(box.startX, box.startY, box.width, box.height)

                // Draw relevant information if the user is hovering over this box
                if (boxCollisions.filter { it.first == i && it.second == j }.any()) {
                    drawBoxOverlay(g, bar, box, labels[j], vals[i][j].toString())
                }
            }
        }
    }

    @SuppressWarnings("MagicNumber")
    private fun drawBoxOverlay(g: Graphics, bar: Array<Box>, box: Box, label: String, labelVal: String) {
        // Color the overlay with a transparent gray
        g.color = Color(0.8F, 0.8F, 0.8F, 0.6F)
        g.fillRect(box.startX, box.startY, box.width, box.height)

        g.color = Color.GRAY
        g.drawString(label, bar.last().startX, bar.last().startY - 20)
        g.drawString("$Y_LABEL=$labelVal", bar.last().startX, bar.last().startY - 10)
    }

    private fun checkCollide(x: Int, y: Int): List<Index2D> {
        val results = mutableListOf<Index2D>()

        for ((i, bar) in boxes.withIndex()) {
            for ((j, box) in bar.withIndex()) {
                if (x > box.startX && x <= box.startX + box.width &&
                    y > box.startY && y <= box.startY + box.height) {
                    results.add(Pair(i, j))
                }
            }
        }

        return results
    }
}

data class Box(
    var startX: Int,
    var startY: Int,
    var width: Int,
    var height: Int
) {
    companion object {
        fun default() = Box(0, 0, 0, 0)
    }
}
