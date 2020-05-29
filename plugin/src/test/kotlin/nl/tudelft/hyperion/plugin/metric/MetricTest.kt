package nl.tudelft.hyperion.plugin.metric

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import nl.tudelft.hyperion.plugin.git.GitLineTracker
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Test class that tests various conversion methods in the metric package.
 */
class MetricTest {
    private val project = mockk<Project>()
    private val file = mockk<VirtualFile>()

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeClass() {
            mockkObject(GitLineTracker)

            every { GitLineTracker.resolveCurrentLine(any(), any(), any(), any()) } returns 0
        }
    }

    @Test
    fun `Test conversion methods for simple case`() {
        val apiMetricsResults = arrayOf(APIMetricsResult(1, mapOf(
                "HEAD" to listOf(APIMetric(0, "DEBUG", 20))
        )))

        val fileMetricsExpected = FileMetrics(mapOf(
                0 to LineMetrics(mapOf(
                        1 to listOf(LineIntervalMetric("HEAD", 20))
                ))
        ))
        val fileMetricsActual = FileMetrics.fromMetricsResults(apiMetricsResults)
        assertEquals(fileMetricsExpected, fileMetricsActual)

        val resolvedFileMetricsExpected = ResolvedFileMetrics(fileMetricsExpected, mapOf(
                0 to mapOf(1 to 20)
        ))
        val resolvedFileMetricsActual = ResolvedFileMetrics.resolve(fileMetricsActual, project, file)
        assertEquals(resolvedFileMetricsExpected, resolvedFileMetricsActual)

    }
}