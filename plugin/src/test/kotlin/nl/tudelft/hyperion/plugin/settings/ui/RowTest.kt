package nl.tudelft.hyperion.plugin.settings.ui

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals

class RowTest {
    @Test
    fun `Test constructor and getColumn & setColumn methods`() {
        val row = Row(1, Period.Seconds);
        assertEquals(1, row.getColumn(0))
        assertEquals(Period.Seconds, row.getColumn(1))
        assertEquals(Period.Seconds, row.getColumn(-1))
        assertEquals(Period.Seconds, row.getColumn(2))
        assertEquals(Period.Seconds, row.getColumn(5000))

        row.setColumn(0, 5)

        assertEquals(5, row.getColumn(0))

        row.setColumn(0, "this should do nothing")

        // Values are incompatible, thus nothing should change
        assertEquals(5, row.getColumn(0))

        row.setColumn(1, Period.Weeks)

        assertEquals(Period.Weeks, row.getColumn(1))

        row.setColumn(1, 42)

        // Values are incompatible, thus nothing should change
        assertEquals(Period.Weeks, row.getColumn(1))
    }

    @TestFactory
    fun `Test various row parses`() = listOf(
            2*7*24*3600 to Row(2, Period.Weeks),
            7*24*3600 to Row(1, Period.Weeks),
            8*24*3600 to Row(8, Period.Days),
            24*3600 to Row(1, Period.Days),
            25*3600 to Row(25, Period.Hours),
            3600 to Row(1, Period.Hours),
            3660 to Row(61, Period.Minutes),
            60 to Row(1, Period.Minutes),
            61 to Row(61, Period.Seconds),
            1 to Row(1, Period.Seconds)
    ).map {
        DynamicTest.dynamicTest("${it.first} should return a Row with interval ${it.second.getColumn(0)}" +
            " and period ${it.second.getColumn(1)}") {
            assertEquals(Row.parse(it.first), it.second)
        }
    }

    @ParameterizedTest
    @EnumSource(Period::class)
    fun `Test various periods with toSeconds() method`(period: Period) {
        assertEquals(period.inSeconds, Row(1, period).toSeconds())
    }

    @Test
    fun `Test clone() and equals() method`() {
        val original = Row(1, Period.Weeks)
        var clone = original.clone()

        assertEquals(original, clone)

        clone.setColumn(0, 2)

        assertNotEquals(original, clone)

        clone = original.clone()

        clone.setColumn(1, Period.Hours)

        assertNotEquals(original, clone)
    }
}
