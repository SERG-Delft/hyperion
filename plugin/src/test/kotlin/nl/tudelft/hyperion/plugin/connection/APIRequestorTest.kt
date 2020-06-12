package nl.tudelft.hyperion.plugin.connection

import com.intellij.openapi.project.Project
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.fullPath
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.plugin.metric.FileMetrics
import nl.tudelft.hyperion.plugin.metric.LineIntervalMetric
import nl.tudelft.hyperion.plugin.metric.LineMetrics
import nl.tudelft.hyperion.plugin.settings.HyperionSettings
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.URLEncoder

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class APIRequestorTest {
    private val mockProject: Project = mockk()

    private var mockClient: HttpClient

    private val testAddress = "http://example.com"
    private val testProject = "TestProject"
    private val testIntervals = listOf(1, 3600, 86400)
    private val filePath = "/path/to/file"

    init {
        val json = """[{"interval":60,"versions":{"abc":[{"line":10,"count":20,"severity":"INFO"}],
            |"def":[{"line":20,"count":1,"severity":"DEBUG"}]}},{"interval":120,"versions":
|           {"abc":[{"line":10,"count":20,"severity":"INFO"}],"def":[{"line":20,"count":1,"severity":"DEBUG"}]}}]"""
            .trimMargin()

        val expectedRequest = "/api/v1/metrics?project=${enc(testProject)}&file=${enc(filePath)}" +
            "&intervals=${enc(testIntervals.joinToString(","))}"
        // Handlers are required to be specified when the client is constructed.
        // Since we only have one test everything is done globally and the client is initialized here.
        mockClient = spyk(HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    when (request.url.fullPath) {
                        expectedRequest -> respondOk(json)
                        else -> respondBadRequest()
                    }
                }
            }
        })
    }

    @AfterAll
    fun cleanup() {
        unmockkAll()
    }

    @Test
    fun `Test correct call`() {

        every { mockProject.name } returns testProject

        every { mockProject.getService(HyperionSettings::class.java) } returns HyperionSettings(mockProject)
            .apply {
                loadState(HyperionSettings.State().apply {
                    intervals = testIntervals
                    address = testAddress
                    project = testProject
                })
            }
        val expected = FileMetrics(
            mapOf(
                10 to LineMetrics(
                    mapOf(
                        60 to listOf(LineIntervalMetric("abc", 20)),
                        120 to listOf(LineIntervalMetric("abc", 20))
                    )
                ),
                20 to LineMetrics(
                    mapOf(
                        60 to listOf(LineIntervalMetric("def", 1)),
                        120 to listOf(LineIntervalMetric("def", 1))
                    )
                )
            )
        )
        runBlocking {
            val result = APIRequestor.getMetricForFile(filePath, mockProject, mockClient)
            assertEquals(expected, result)
        }
    }

    fun enc(string: String): String {
        return URLEncoder.encode(string, "utf-8")
    }
}
