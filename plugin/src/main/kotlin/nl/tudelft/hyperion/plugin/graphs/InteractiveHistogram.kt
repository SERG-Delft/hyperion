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

/**
 * Represents a Swing-based histogram with overlay drawing on hover and
 * optional mouse actions.
 * The histogram resizes automatically to the parent container's size.
 *
 * @constructor
 * Add all action listeners.
 *
 * @param initialData the initial data values for the histogram.
 *  The frequency array should be have bars*boxes as size.
 * @property xMargin the left and right margin in pixels.
 * @property yMargin the top and bottom margin in pixels.
 * @property barSpacing the spacing between bars in pixels.
 * @property bottomY the Y coordinate of the bottom of the histogram.
 * @property topY the Y coordinate of the top of the histogram.
 */
class InteractiveHistogram(
    initialData: HistogramData,
    private val xMargin: Int,
    private val yMargin: Int,
    private var barSpacing: Int
) : JPanel(true) {

    var bars: Array<Bar> = initialData.frequency.map { Bar(it.size) }.toTypedArray()

    var collidingBox: Box? = null
    var collidingBar: Bar? = null
    var isCurrentlyColored = false

    /**
     * A 90° flipped version of the font used by the histogram.
     */
    private val verticalFont: Font

    /**
     * Represents the Y coordinate of the bottom of the histogram.
     */
    private val bottomY
        get() = height - yMargin

    /**
     * Represents the Y coordinate of the top of the histogram.
     */
    private val topY
        get() = yMargin

    /**
     * The data to display in the histogram.
     * Change array size of [bars] if this value changes.
     */
    var data = initialData
        set(value) {
            field = value

            // Create array of uninitialized boxes
            bars = value.frequency.map { Bar(it.size) }.toTypedArray()
        }

    init {
        this.addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent?) {
                val (bar, box) = checkCollide(e?.x!!, e.y)
                collidingBar = bar
                collidingBox = box

                // Only redraw if the cursor is hovering over any of the boxes
                if (bar != null || box != null) {
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

        // Set font used
        font = Font("Monospaced", Font.PLAIN, 10)

        // Create vertical font from base font
        val affineTransform = AffineTransform()
        affineTransform.rotate(-PI / 2, 0.0, 0.0)
        verticalFont = font.deriveFont(affineTransform)
    }

    companion object {
        // What text to put on the Y axis
        const val Y_AXIS_LABEL = "count"
        const val Y_AXIS_LABEL_SPACING = 10

        // Color the overlay with a transparent gray
        val OVERLAY_COLOR = Color(0.8F, 0.8F, 0.8F, 0.6F)

        val LABEL_COLOR = Color.GRAY
    }

    /**
     * Updates the data and view of the histogram.
     *
     * @param data the name data to display in the histogram.
     */
    fun update(data: HistogramData) {
        this.data = data
        repaint()
    }

    /**
     * Calculates the positions of the bars and boxes based on the given
     * histogram data. The height of the bars is scaled linearly to fit in the
     * max height of the histogram, which is the difference between [bottomY]
     * and [topY].
     *
     * Note that it is assumed that the dimensions of [HistogramData.frequency] matches those of
     * [bars] times its [Bar.boxes] property.
     */
    private fun calculateBoxes() {
        val histogramWidth = width - 2 * xMargin
        val barWidth = histogramWidth / data.frequency.size

        val maxBarTotal = data.frequency.map(Array<Int>::sum).max()
        val barHeightScale = (height - 2 * yMargin) / maxBarTotal!!.toDouble()

        for ((i, bar) in bars.withIndex()) {
            var prevY = 0
            val leftMargin = xMargin + i * barWidth

            bar.startX = leftMargin + barSpacing / 2
            bar.width = barWidth - barSpacing

            for ((j, box) in bar.boxes.withIndex()) {
                // Normalize the height based on the Y positions
                val currentY = round(data.frequency[i][j] * barHeightScale).toInt()

                box.startY = height - yMargin - (prevY + currentY)
                box.height = currentY

                prevY += currentY
            }
        }
    }

    /**
     * Draws all parts of the histogram component.
     *
     * @param g the [Graphics] object used to draw on the buffer.
     */
    @SuppressWarnings("MagicNumber")
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        // Draw X and Y axis
        g.color = LABEL_COLOR
        g.drawLine(xMargin, bottomY, width - xMargin, bottomY)
        g.drawLine(xMargin, bottomY, xMargin, topY)

        val g2 = g as Graphics2D

        // Set anti aliasing
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        drawHistogram(g)
    }

    /**
     * Draws the timestamps on the X-axis and also calculates the positions of
     * all boxes and draws them.
     *
     * @param g the [Graphics] object used to draw on the buffer.
     */
    private fun drawHistogram(g: Graphics) {
        // TODO: only recalculate boxes on component show or component resize
        if (data.frequency.isNotEmpty()) {
            calculateBoxes()
        }

        // TODO: nicely parse the number with a suffix, e.g. 10_000 -> 10K
        drawYAxisLabels(g, "0", data.frequency.map { it.sum() }.max().toString())

        for ((i, bar) in bars.withIndex()) {

            // Draw timestamps
            drawTimeStamp(g, data.timestamps[i], bar)

            if (bar === collidingBar && collidingBox == null) {
                drawBarOverlay(g, bar, data.frequency[i].sum().toString())
            }

            // Draw bars
            for ((j, box) in bar.boxes.withIndex()) {
                g.color = data.colors[i][j]
                g.fillRect(bar.startX, box.startY, bar.width, box.height)

                // Draw relevant information if the user is hovering over this box
                if (box === collidingBox) {
                    drawBoxOverlay(g, bar, box, data.labels[i][j], data.frequency[i][j].toString())
                }
            }
        }
    }

    /**
     * Draws the x-axis timestamp values.
     * The given text is split over multiple lines if it contains a new line.
     *
     * @param g the [Graphics] object used to draw on the buffer.
     * @param text the timestamp or label to draw.
     * @param bar which bar to draw the timestamp after.
     */
    private fun drawTimeStamp(g: Graphics, text: String, bar: Bar) {
        val xLabelFontMetrics = g.getFontMetrics(font)
        g.color = LABEL_COLOR

        val lines = text.split("\n")

        for ((i, line) in lines.withIndex()) {
            g.drawString(
                line,
                bar.startX + bar.width - xLabelFontMetrics.stringWidth(line) / 2,
                bottomY + xLabelFontMetrics.height + i * xLabelFontMetrics.height
            )
        }
    }

    /**
     * Draws a value at the top and bottom of the Y-axis.
     *
     * @param g the [Graphics] object used to draw on the buffer.
     * @param minLabel the string to draw at the bottom.
     * @param maxLabel the string to draw at the top.
     */
    private fun drawYAxisLabels(g: Graphics, minLabel: String, maxLabel: String) {
        g.color = LABEL_COLOR
        val maxLabelWidth = g.fontMetrics.stringWidth(maxLabel)
        g.drawString(
            maxLabel,
            xMargin - maxLabelWidth - Y_AXIS_LABEL_SPACING,
            topY + g.fontMetrics.height / 2
        )
        g.drawString(
            minLabel,
            xMargin - g.fontMetrics.stringWidth(minLabel) - Y_AXIS_LABEL_SPACING,
            bottomY + g.fontMetrics.height / 2
        )

        // Draw Y-axis label
        g.font = verticalFont
        val yLabelLength = g.getFontMetrics(font).stringWidth(Y_AXIS_LABEL)
        g.drawString(Y_AXIS_LABEL, xMargin - Y_AXIS_LABEL_SPACING - maxLabelWidth, (height + yLabelLength) / 2)
        g.font = font
    }

    /**
     * Draws a rectangle overlay over the given box and the corresponding
     * textual information above the bar.
     *
     * @param g the [Graphics] object to draw on the buffer.
     * @param bar the [Bar] to draw text above, necessary for the coordinates.
     * @param box the [Box] to dray a rectangle on.
     * @param label what to label this particular bin.
     * @param labelVal the value of this particular bin.
     */
    @SuppressWarnings("MagicNumber")
    private fun drawBoxOverlay(g: Graphics, bar: Bar, box: Box, label: String, labelVal: String) {
        // Color the overlay with a transparent gray
        g.color = OVERLAY_COLOR
        g.fillRect(bar.startX, box.startY, bar.width, box.height)

        g.color = LABEL_COLOR
        g.drawString(label, bar.startX, bar.boxes.last().startY - 20)
        g.drawString("$Y_AXIS_LABEL=$labelVal", bar.startX, bar.boxes.last().startY - 10)
    }

    /**
     * Draws a rectangle overlay over the given bar and the corresponding
     * textual information above the bar.
     *
     * @param g the [Graphics] object to draw on the buffer.
     * @param bar the [Bar] to draw text above, necessary for the coordinates.
     * @param labelVal the value of this particular bin.
     */
    @SuppressWarnings("MagicNumber")
    private fun drawBarOverlay(g: Graphics, bar: Bar, labelVal: String) {
        g.color = OVERLAY_COLOR
        g.fillRect(bar.startX, topY - 10, bar.width, height - 2 * yMargin + 10)

        g.color = LABEL_COLOR
        g.drawString("$Y_AXIS_LABEL=$labelVal", bar.startX, topY - g.fontMetrics.height)
    }

    /**
     * Checks if the given coordinates are within any of the boxes or bars.
     *
     * @param x the x coordinate to check collisions of.
     * @param y the x coordinate to check collisions of.
     * @return a pair of the first colliding bar and box, otherwise null.
     */
    private fun checkCollide(x: Int, y: Int): Pair<Bar?, Box?> {
        for (bar in bars) {
            if (x <= bar.startX || x > bar.startX + bar.width) {
                continue
            }

            for (box in bar.boxes) {
                if (y > box.startY && y <= box.startY + box.height) {
                    return Pair(bar, box)
                }
            }

            return Pair(if (y <= bottomY) bar else null , null)
        }

        return Pair(null, null)
    }
}

/**
 * Represents a single bar in a histogram composed of multiple levels of boxes.
 * Stores the X parameters and the child [Box]-s.
 */
data class Bar(
    var startX: Int = 0,
    var width: Int = 0,
    var boxes: List<Box>
) {
    /**
     * Fills [boxes] with [boxCount] of default boxes.
     */
    constructor(boxCount: Int) : this(boxes = (0 until boxCount).map { Box() })
}

/**
 * Represents a single box which tracks the Y coordinate and height, the X
 * parameters are tracked by the parent [Bar].
 */
data class Box(
    var startY: Int = 0,
    var height: Int = 0
)
