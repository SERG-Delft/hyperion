package nl.tudelft.hyperion.plugin.graphs

import java.awt.event.MouseEvent
import javax.swing.event.MouseInputAdapter

/**
 * Information of where the user clicked inside the histogram.
 *
 * @property barIndex the index of which bar was clicked.
 * @property boxIndex a possible index if a box was clicked.
 * @property binComponent possible [BinComponent], is null when the whole bar
 *  is clicked and not null when a box is clicked.
 */
data class ClickContext(
    val barIndex: Int,
    val boxIndex: Int? = null,
    val binComponent: BinComponent? = null
) {
    val isWholeBarClicked
        get() = boxIndex == null && binComponent == null
}

/**
 * Callback for when a box or bar is clicked.
 * T must be either a Pair of indices or an Index which refers to the bar that
 * was clicked, it cannot be restricted via Kotlin's type system.
 */
typealias clickedComponentCallback = (ClickContext) -> Unit

/**
 * Represents a Swing-based histogram with overlay drawing on hover, mouse
 * actions and clickable boxes.
 * The histogram resizes automatically to the parent container's size.
 *
 * @constructor
 * Add all action listeners.
 *
 * @param initialData the initial data values for the histogram.
 *  The frequency array should be have bars*boxes as size.
 * @param xMargin the left and right margin in pixels.
 * @param yMargin the top and bottom margin in pixels.
 * @param barSpacing the spacing between bars in pixels.
 * @param clickCallback a callback that is executed when the user clicks on a
 *  [BinComponent].
 */
class ClickableHistogram(
    initialData: HistogramData,
    xMargin: Int,
    yMargin: Int,
    barSpacing: Int,
    clickCallback: clickedComponentCallback
) : InteractiveHistogram(initialData, xMargin, yMargin, barSpacing) {
    init {
        this.addMouseListener(object : MouseInputAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                val i = if (collidingBar != null) {
                    bars.indexOfFirst { it === collidingBar }
                } else -1

                if (collidingBox != null && i != -1) {
                    val j = bars[i].boxes.indexOfFirst { it === collidingBox }
                    check(j != -1) {
                        "collidingBox $collidingBox is a 'dangling' reference and does not exist in collidingBar"
                    }
                    clickCallback(ClickContext(i, j, data.bins[i][j]))
                } else if (i != -1) {
                    clickCallback(ClickContext(i))
                }
            }
        })
    }
}
