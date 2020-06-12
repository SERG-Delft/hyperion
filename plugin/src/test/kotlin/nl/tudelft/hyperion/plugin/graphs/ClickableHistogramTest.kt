package nl.tudelft.hyperion.plugin.graphs

import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.awt.Color
import java.awt.event.MouseEvent
import javax.swing.JPanel
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClickableHistogramTest {

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
    fun `clickCallback should be executed when there is a collidingBox`() {
        var executed = false
        val cb: (ClickContext) -> Unit = {
            assertTrue { it.barIndex == 0 }
            assertNull(it.binComponent)
            assertNull(it.boxIndex)
            executed = true
        }

        val hist = ClickableHistogram(HistogramData(arrayOf(arrayOf()), arrayOf()), 0, 0, 0, cb)

        val bar = Bar(0)
        hist.bars = arrayOf(bar)
        hist.collidingBar = bar

        val mockEvent = mockk<MouseEvent>(relaxed = true)

        hist.mouseListeners.first().mouseClicked(mockEvent)

        // spyk-ing the lambda results in Unresolved reference, so this method must be used
        assertTrue(executed)
    }

    @Test
    fun `clickCallback should be executed with box context if collidingBox is not null`() {
        val binComponent = BinComponent(1, Color.BLACK, "WARN")

        var executed = false
        val cb: (ClickContext) -> Unit = {
            assertTrue { it.barIndex == 0 }
            assertTrue { it.boxIndex == 0 }
            assertEquals(binComponent, it.binComponent)
            executed = true
        }

        val hist = ClickableHistogram(HistogramData(arrayOf(arrayOf(binComponent)), arrayOf()), 0, 0, 0, cb)

        val box = Box()
        val bar = Bar(boxes = listOf(box))
        hist.bars = arrayOf(bar)
        hist.collidingBar = bar
        hist.collidingBox = box

        val mockEvent = mockk<MouseEvent>(relaxed = true)
        hist.mouseListeners.first().mouseClicked(mockEvent)

        // spyk-ing the lambda results in Unresolved reference, so this method must be used
        assertTrue(executed)
    }

    @Test
    fun `clickCallback should throw exception when unknown box is collided`() {
        val binComponent = BinComponent(1, Color.BLACK, "WARN")

        val cb: (ClickContext) -> Unit = {}

        val hist = ClickableHistogram(HistogramData(arrayOf(arrayOf(binComponent)), arrayOf()), 0, 0, 0, cb)
        val box = Box()
        val bar = Bar(boxes = listOf(Box()))
        hist.bars = arrayOf(bar)
        hist.collidingBar = bar
        hist.collidingBox = box

        val mockEvent = mockk<MouseEvent>(relaxed = true)

        assertThrows<IllegalStateException> {
            hist.mouseListeners.first().mouseClicked(mockEvent)
        }
    }
}