package nl.tudelft.hyperion.plugin.metric

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import nl.tudelft.hyperion.plugin.git.GitLineTracker
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals

/**
 * Test class that tests various conversion methods in the metric package.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetricConversionTest {
    private val project = mockk<Project>()
    private val file = mockk<VirtualFile>()

    init {
        mockkObject(GitLineTracker)
    }

    @AfterAll
    fun cleanup() {
        unmockkAll()
    }

    @Test
    fun `Test conversion methods for simple case`() {
        // Set necessary return value for line resolving.
        every { GitLineTracker.resolveCurrentLine(any(), any(), any(), any()) } returns 0

        val apiMetricsResults = arrayOf(
            APIMetricsResult(
                1, mapOf(
                "HEAD" to listOf(APIMetric(0, "DEBUG", 20))
            )
            )
        )

        val fileMetricsExpected = FileMetrics(
            mapOf(
                0 to LineMetrics(
                    mapOf(
                        1 to listOf(LineIntervalMetric("HEAD", 20))
                    )
                )
            )
        )
        val fileMetricsActual = FileMetrics.fromMetricsResults(apiMetricsResults)
        assertEquals(fileMetricsExpected, fileMetricsActual)

        val lineSumsExpected = mapOf(
            0 to mapOf(1 to 20)
        )
        val resolvedFileMetricsExpected = ResolvedFileMetrics(fileMetricsExpected, lineSumsExpected)
        val resolvedFileMetricsActual = ResolvedFileMetrics.resolve(fileMetricsActual, project, file)

        assertEquals(fileMetricsExpected, resolvedFileMetricsActual.metrics)
        assertEquals(lineSumsExpected, resolvedFileMetricsActual.lineSums)
        assertEquals(resolvedFileMetricsExpected, resolvedFileMetricsActual)
    }

    @Test
    fun `Test conversion methods with unresolved line`() {
        // Set necessary return value for line resolving, in this case we return null as if no line could be resolved.
        every { GitLineTracker.resolveCurrentLine(any(), any(), any(), any()) } returns null

        val fileMetrics = FileMetrics(
            mapOf(
                0 to LineMetrics(
                    mapOf(
                        1 to listOf(LineIntervalMetric("HEAD", 20))
                    )
                )
            )
        )
        val resolvedFileMetricsExpected = ResolvedFileMetrics(fileMetrics, emptyMap())
        val resolvedFileMetricsActual = ResolvedFileMetrics.resolve(fileMetrics, project, file)

        assertEquals(resolvedFileMetricsExpected, resolvedFileMetricsActual)
    }
}
