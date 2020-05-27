package nl.tudelft.hyperion.plugin

import nl.tudelft.hyperion.plugin.metric.IntervalMetric
import nl.tudelft.hyperion.plugin.metric.LineMetrics
import nl.tudelft.hyperion.plugin.metric.APIMetric
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LineMetricsTest {

    @Test
    fun `Test LineMetrics Methods`() {
        val metricOne = IntervalMetric(60, APIMetric(20, 20, Severity.INFO))
        val metricTwo = IntervalMetric(7000, APIMetric(20, 3000, Severity.INFO))
        val metricThree = IntervalMetric(3600*25, APIMetric(20, 3999, Severity.INFO))

        val lineMetrics = LineMetrics(listOf(metricOne, metricTwo, metricThree))

        assertEquals("[20 last 1 min]  [3000 last 1 h 56 min 40 s]  [3999 last 1 d 1 h]", lineMetrics.getText())

        assertEquals(20, lineMetrics.getLine(), "All metrics should be on line 20")

        lineMetrics.setLine(21)
        assertEquals(21, lineMetrics.getLine(), "After change all metrics should be on line 21")

        for ((i, metric) in lineMetrics.metrics.withIndex()) {
            assertEquals(21, metric.metric.line, "Metric ${i+1} should be on line 21")
        }
    }
    @Test
    fun `Test Constructor`() {
        val metric = APIMetric(0, 21, Severity.DEBUG)
        val intervalMetric = IntervalMetric(20, metric)
        assertEquals(20, intervalMetric.interval, "Interval should be 20")
        assertEquals(metric, intervalMetric.metric,
                "Metric should be with on line 0 with count 21 and debug severity")

    }
    @Test
    fun `Test Metric count for getText`() {
        val metric = APIMetric(0, 21, Severity.INFO)
        val intervalMetric = IntervalMetric(0, metric)
        assertTrue(intervalMetric.getText().startsWith(21.toString()), "Metric should have count 21")
    }
    @TestFactory
    fun `Check string for various single intervals`() = listOf(
            0 to "0 s",
            59 to "59 s",
            60 to "1 min",
            60*59 to "59 min",
            3600 to "1 h",
            3600*23 to "23 h",
            3600*24 to "1 d",
            3600*24*6 to "6 d",
            3600*24*7 to "1 w"
    ).map { dynamicIntervalTest(it.first, it.second) }

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
    ).map {dynamicIntervalTest(it.first, it.second)}
    private fun dynamicIntervalTest(input: Int, expected: String): DynamicTest {
        val actual = IntervalMetric(input,
                                    APIMetric(0, 0, Severity.INFO)).getFormattedInterval()
        return DynamicTest.dynamicTest("Metric with $input second interval should return\"$expected\"") {
            assertEquals(expected, actual)
        }
    }
}
