package nl.tudelft.hyperion.plugin.components

import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import nl.tudelft.hyperion.plugin.graphs.BinComponent
import nl.tudelft.hyperion.plugin.graphs.ClickContext
import nl.tudelft.hyperion.plugin.graphs.FileScope
import nl.tudelft.hyperion.plugin.graphs.HistogramInterval
import nl.tudelft.hyperion.plugin.graphs.HistogramSettings
import nl.tudelft.hyperion.plugin.graphs.ProjectScope
import nl.tudelft.hyperion.plugin.metric.APIBinMetricsResponse
import nl.tudelft.hyperion.plugin.metric.APIBinMetricsResult
import nl.tudelft.hyperion.plugin.metric.APIMetric
import nl.tudelft.hyperion.plugin.metric.FileAPIMetric
import nl.tudelft.hyperion.plugin.settings.HyperionSettings
import nl.tudelft.hyperion.plugin.visualization.VisToolWindowFactory
import nl.tudelft.hyperion.plugin.visualization.components.CodeList
import nl.tudelft.hyperion.plugin.visualization.components.VisWindow
import nl.tudelft.hyperion.plugin.visualization.components.clickHandler
import nl.tudelft.hyperion.plugin.visualization.components.createBinLineInfo
import nl.tudelft.hyperion.plugin.visualization.components.createSortedTableEntries
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.awt.Color
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VisWindowClickHandlerTest {

    @AfterEach
    fun cleanup() {
        unmockkAll()
    }

    /**
     * Mocks [HyperionSettings] to always return [state].
     */
    private fun mockProject(state: HyperionSettings.State) {
        mockkObject(HyperionSettings.Companion)
        mockkStatic(ProjectManager::class)

        val mockProject = mockk<ProjectManager>(relaxed = true)

        every {
            ProjectManager.getInstance()
        } returns mockProject

        every {
            mockProject.openProjects
        } returns arrayOf(mockk(relaxed = true))

        val mockSettings = mockk<HyperionSettings>(relaxed = true)

        every {
            HyperionSettings.getInstance(any())
        } returns mockSettings

        every {
            mockSettings.state
        } returns state
    }

    @Test
    fun `createSortedTableEntries() should sort rows`() {
        val state = HyperionSettings.State()
        state.visualization = HistogramSettings(
            HistogramInterval.Hour,
            scope = FileScope("src/main/java/Foo.java")
        )

        mockProject(state)

        val apiMetrics = listOf(
            APIMetric(1, "WARN", 1),
            APIMetric(2, "INFO", 4)
        )

        val expected = listOf(
            CodeList.Companion.TableEntry("src/main/java/Foo.java", "Foo.java", "2", "INFO", "4"),
            CodeList.Companion.TableEntry("src/main/java/Foo.java", "Foo.java", "1", "WARN", "1")
        )

        val rows = createSortedTableEntries(apiMetrics)

        assertEquals(expected, rows)
    }

    @Test
    fun `createSortedTableEntries() should throw error if any metrics are missing file field when project wide`() {
        val state = HyperionSettings.State()
        state.visualization = HistogramSettings(
            HistogramInterval.Hour,
            scope = ProjectScope
        )

        mockProject(state)

        val apiMetrics = listOf(
            FileAPIMetric(1, "WARN", 1, "src/main/java/Bar.java"),
            APIMetric(2, "INFO", 4)
        )

        assertThrows<IllegalStateException> {
            createSortedTableEntries(apiMetrics)
        }
    }

    @Test
    fun `createSortedTableEntries() should sort FileAPIMetric correctly`() {
        val state = HyperionSettings.State()
        state.visualization = HistogramSettings(
            HistogramInterval.Hour,
            scope = ProjectScope
        )

        mockProject(state)

        val apiMetrics = listOf(
            FileAPIMetric(1, "WARN", 1, "src/main/java/Foo.java"),
            FileAPIMetric(2, "INFO", 4, "src/main/java/package/Bar.java")
        )

        val expected = listOf(
            CodeList.Companion.TableEntry("src/main/java/package/Bar.java", "Bar.java", "2", "INFO", "4"),
            CodeList.Companion.TableEntry("src/main/java/Foo.java", "Foo.java", "1", "WARN", "1")
        )

        val rows = createSortedTableEntries(apiMetrics)

        assertEquals(expected, rows)
    }

    @Test
    fun `createBinLineInfo should return empty list when no metrics are available`() {
        val state = HyperionSettings.State()
        state.visualization = HistogramSettings(
            HistogramInterval.Hour,
            scope = FileScope("src/main/kotlin/Foo.kt")
        )

        mockProject(state)

        VisWindow.branchVersion = "d9cd8155764c3543f10fad8a480d743137466f8d55213c8eaefcd12f06d43a80"
        VisWindow.apiMetrics = APIBinMetricsResponse(
            10,
            listOf(
                APIBinMetricsResult(0, mutableMapOf(VisWindow.branchVersion to listOf()))
            )
        )

        val info = createBinLineInfo(0)

        assertNotNull(info)
        assertTrue { info.second.isEmpty() }
    }

    @Test
    fun `createBinLineInfo should return null when apiMetrics are not available`() {
        val state = HyperionSettings.State()
        state.visualization = HistogramSettings(
            HistogramInterval.Hour,
            scope = FileScope("src/main/kotlin/Foo.kt")
        )

        mockProject(state)

        VisWindow.branchVersion = "d9cd8155764c3543f10fad8a480d743137466f8d55213c8eaefcd12f06d43a80"
        VisWindow.apiMetrics = APIBinMetricsResponse(
            10,
            listOf(
                APIBinMetricsResult(0, mutableMapOf())
            )
        )

        val info = createBinLineInfo(0)

        assertNull(info)
    }

    @Test
    fun `createBinLineInfo should return title and rows`() {
        val state = HyperionSettings.State()
        state.visualization = HistogramSettings(
            HistogramInterval.Hour,
            scope = ProjectScope
        )

        mockProject(state)

        val apiMetrics = listOf(
            FileAPIMetric(1, "WARN", 1, "src/main/java/Foo.java"),
            FileAPIMetric(2, "INFO", 4, "src/main/java/package/Bar.java")
        )

        val expected = listOf(
            CodeList.Companion.TableEntry("src/main/java/package/Bar.java", "Bar.java", "2", "INFO", "4"),
            CodeList.Companion.TableEntry("src/main/java/Foo.java", "Foo.java", "1", "WARN", "1")
        )

        VisWindow.branchVersion = "d9cd8155764c3543f10fad8a480d743137466f8d55213c8eaefcd12f06d43a80"
        VisWindow.apiMetrics = APIBinMetricsResponse(
            10,
            listOf(
                APIBinMetricsResult(0, mutableMapOf(VisWindow.branchVersion to apiMetrics))
            )
        )

        val info = createBinLineInfo(0)

        assertEquals(expected, info!!.second)
    }

    @Test
    fun `clickHandler() should return filtered box info depending on click context`() {
        val state = HyperionSettings.State()
        state.visualization = HistogramSettings(
            HistogramInterval.Hour,
            scope = ProjectScope
        )

        mockProject(state)

        val apiMetrics = listOf(
            FileAPIMetric(1, "WARN", 1, "src/main/java/Foo.java"),
            FileAPIMetric(2, "INFO", 4, "src/main/java/package/Bar.java")
        )

        val expected = listOf(
            CodeList.Companion.TableEntry("src/main/java/Foo.java", "Foo.java", "1", "WARN", "1")
        )

        VisWindow.branchVersion = "d9cd8155764c3543f10fad8a480d743137466f8d55213c8eaefcd12f06d43a80"
        VisWindow.apiMetrics = APIBinMetricsResponse(
            10,
            listOf(
                APIBinMetricsResult(0, mutableMapOf(VisWindow.branchVersion to apiMetrics))
            )
        )

        // Mock the tool windows
        mockkObject(ToolWindowManager.Companion)

        val mockManager = mockk<ToolWindowManager>()
        every {
            ToolWindowManager.getInstance(any())
        } returns mockManager

        every {
            mockManager.getToolWindow(any())
        } returns null

        // Mock the tool window tab
        val mockTab = mockk<CodeList>(relaxed = true)
        VisToolWindowFactory.codeListTab = mockTab

        clickHandler(ClickContext(0, 0, BinComponent(10, Color.BLACK, "WARN")))

        verify {
            mockTab.updateTable(any(), expected)
        }
    }
}