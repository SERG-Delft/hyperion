package nl.tudelft.hyperion.plugin.graphs

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.awt.Color
import java.awt.Graphics
import javax.swing.JPanel
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class InteractiveHistogramTest {

    companion object {
        @BeforeAll
        fun setUp() {
            mockkStatic(JPanel::class)
        }

        @AfterEach
        fun cleanup() {
            unmockkAll()
        }
    }

    @Test
    fun `checkCollide() should return only the bar when no box is selected`() {
        val hist = spyk(InteractiveHistogram(HistogramData(arrayOf(arrayOf()), arrayOf()), 0, 0, 0))

        every {
            hist.height
        } returns 10

        hist.bars = arrayOf(
            Bar(0, 10, listOf(Box(0, 2)))
        )

        val (collidingBar, collidingBox) = hist.checkCollide(5, 5)

        assertNotNull(collidingBar)
        assertNull(collidingBox)
    }

    @Test
    fun `checkCollide() should return not return anything if the coordinates are outside of the histogram bounds`() {
        val hist = spyk(InteractiveHistogram(HistogramData(arrayOf(arrayOf()), arrayOf()), 0, 0, 0))

        every {
            hist.height
        } returns 10

        hist.bars = arrayOf(
            Bar(0, 10, listOf(Box(0, 2)))
        )

        val (collidingBar, collidingBox) = hist.checkCollide(5, 20)

        assertNull(collidingBar)
        assertNull(collidingBox)
    }

    @Test
    fun `checkCollide() should return a box when selected`() {
        val hist = spyk(InteractiveHistogram(HistogramData(arrayOf(arrayOf()), arrayOf()), 0, 0, 0))

        every {
            hist.height
        } returns 10

        val expectedBox = Box(2, 4)
        val expectedBar = Bar(0, 10, listOf(Box(0, 2), expectedBox, Box(6, 2)))

        hist.bars = arrayOf(
            expectedBar
        )

        val result = hist.checkCollide(5, 3)

        assertEquals(Pair(expectedBar, expectedBox), result)
    }

    @Test
    fun `checkCollide() should return nothing if all is missed`() {
        val hist = spyk(InteractiveHistogram(HistogramData(arrayOf(arrayOf()), arrayOf()), 0, 0, 0))

        every {
            hist.height
        } returns 10

        hist.bars = arrayOf(
            Bar(0, 10, listOf(Box(0, 2), Box(6, 2))),
            Bar(10, 10, listOf(Box(0, 2), Box(6, 2))),
            Bar(20, 10, listOf(Box(0, 2), Box(6, 2)))
        )

        val result = hist.checkCollide(31, 3)

        assertEquals(Pair(null, null), result)
    }

    @Test
    fun `drawHistogram() should draw a single bar on data with a single bar`() {
        val timestamp = "10:00:00"

        val hist = spyk(InteractiveHistogram(HistogramData(arrayOf(arrayOf()), arrayOf(timestamp)), 0, 0, 0))
        val mockGraphics = mockk<Graphics>(relaxed = true)

        hist.drawHistogram(mockGraphics)

        verify {
            hist.calculateBoxes()
            hist.drawTimeStamp(mockGraphics, timestamp, any())
        }
    }

    @Test
    fun `drawHistogram() should skip drawing and calculating on empty data`() {
        val hist = spyk(InteractiveHistogram(HistogramData(arrayOf(), arrayOf()), 0, 0, 0))
        val mockGraphics = mockk<Graphics>(relaxed = true)

        hist.drawHistogram(mockGraphics)

        verify(exactly = 0) {
            hist.calculateBoxes()
            hist.drawTimeStamp(mockGraphics, any(), any())
        }
    }

    @Test
    fun `drawHistogram() should draw a bar overlay if a bar is collided but no box`() {
        val hist = spyk(InteractiveHistogram(HistogramData(arrayOf(arrayOf()), arrayOf("foo")), 0, 0, 0))
        val mockGraphics = mockk<Graphics>(relaxed = true)

        val bar = hist.bars.first()
        hist.collidingBar = bar

        hist.drawHistogram(mockGraphics)

        verify {
            hist.drawBarOverlay(mockGraphics, bar, any())
        }
    }

    @Test
    fun `drawHistogram() should draw a box overlay if a box is collided`() {
        val label = "WARN"
        val labelVal = 0

        val hist = spyk(
            InteractiveHistogram(
                HistogramData(
                    arrayOf(arrayOf(BinComponent(labelVal, Color.BLACK, label))),
                    arrayOf("foo")
                ),
                0, 0, 0
            )
        )
        val mockGraphics = mockk<Graphics>(relaxed = true)

        val bar = hist.bars.first()
        val box = bar.boxes.first()
        hist.collidingBar = bar
        hist.collidingBox = box

        hist.drawHistogram(mockGraphics)

        verify(exactly = 0) {
            hist.drawBarOverlay(any(), any(), any())
        }

        verify {
            hist.drawBoxOverlay(mockGraphics, bar, box, label, labelVal.toString())
        }
    }
}