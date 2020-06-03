package nl.tudelft.hyperion.plugin.doc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class MetricInlayItemTest {
    @TestFactory
    fun `Check string for combination intervals`() = listOf(
            61 to "1 min 1 s",
            3601 to "1 h 1 s",
            3660 to "1 h 1 min",
            3661 to "1 h 1 min 1 s",
            3600*24+1 to "1 d 1 s",
            3600*24+60 to "1 d 1 min",
            3600*24+61 to "1 d 1 min 1 s",
            3600*25 to "1 d 1 h",
            3600*24+3660 to "1 d 1 h 1 min",
            3600*24+3661 to "1 d 1 h 1 min 1 s",
            3600*24*7+1 to "1 w 1 s",
            3600*24*7+60 to "1 w 1 min",
            3600*24*7+61 to "1 w 1 min 1 s",
            3600*24*7+3600 to "1 w 1 h",
            3600*24*7+3660 to "1 w 1 h 1 min",
            3600*24*7+3661 to "1 w 1 h 1 min 1 s",
            3600*24*8 to "1 w 1 d",
            3600*24*8+1 to "1 w 1 d 1 s",
            3600*24*8+60 to "1 w 1 d 1 min",
            3600*24*8+61 to "1 w 1 d 1 min 1 s",
            3600*24*8+3600 to "1 w 1 d 1 h",
            3600*24*8+3601 to "1 w 1 d 1 h 1 s",
            3600*24*8+3660 to "1 w 1 d 1 h 1 min",
            3600*24*8+3661 to "1 w 1 d 1 h 1 min 1 s"
    ).map {DynamicTest.dynamicTest("An interval of ${it.first} should return label ${it.second}") {
        assertEquals("[0 last ${it.second}]", countsToLabel(mapOf(it.first to 0)))
    } }
}
