package nl.tudelft.hyperion.plugin.metric

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * This test class tests the Metric constructors.
 */
class MetricUnitTest {
    @Test
    fun `Test APIMetric & APIMetricsResult constructor`() {
        // Construct the APIMetric.
        val line = 0
        val severity = "INFO"
        val count = 20
        val apiMetric = APIMetric(line, severity, count)

        // Check if all properties are as they should be.
        assertEquals(line, apiMetric.line)
        assertEquals(severity, apiMetric.severity)
        assertEquals(count, apiMetric.count)

        // Construct the APIMetricsResult.
        val interval = 60
        val versions = mapOf("ABC" to listOf(apiMetric))
        val apiMetricsResult = APIMetricsResult(interval, versions)

        // Check if all properties are as they should be.
        assertEquals(interval, apiMetricsResult.interval)
        assertEquals(versions, apiMetricsResult.versions)
        assertEquals(apiMetric, apiMetricsResult.versions["ABC"]?.first())
    }

    @Test
    fun `Test Metric & ResolvedFileMetrics constructor`() {
        // Construct the LineIntervalMetric.
        val version = "ABC"
        val count = 20
        val lineIntervalMetric = LineIntervalMetric(version, count)

        // Check if all properties are as they should be.
        assertEquals(version, lineIntervalMetric.version)
        assertEquals(count, lineIntervalMetric.count)

        // Construct the LineMetrics.
        val interval = 60
        val intervals = mapOf(interval to listOf(lineIntervalMetric))
        val lineMetrics = LineMetrics(intervals)

        // Check if all properties are as they should be.
        assertEquals(intervals, lineMetrics.intervals)
        assertEquals(lineIntervalMetric, lineMetrics.intervals[interval]?.first())

        // Construct the FileMetrics.
        val line = 0
        val lines = mapOf(line to lineMetrics)
        val fileMetrics = FileMetrics(lines)

        // Check if all properties are as they should be.
        assertEquals(lines, fileMetrics.lines)
        assertEquals(lineMetrics, fileMetrics.lines[line])

        // Construct the ResolvedFileMetrics.
        val lineSums = mapOf(line to mapOf(interval to count))
        val resolvedFileMetrics = ResolvedFileMetrics(fileMetrics, lineSums)

        // Check if all properties are as they should be.
        assertEquals(fileMetrics, resolvedFileMetrics.metrics)
        assertEquals(lineSums, resolvedFileMetrics.lineSums)
    }
}
