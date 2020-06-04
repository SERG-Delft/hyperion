package nl.tudelft.hyperion.plugin.settings.ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import nl.tudelft.hyperion.plugin.settings.HyperionSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HyperionSettingsFormTest {
    private val mockProject: Project = mockk()
    private val hyperionSettings: HyperionSettings
    private lateinit var hyperionSettingsForm: HyperionSettingsForm
    private val createTableMethod = getMethod("createTable")

    init {
        every { mockProject.name } returns "TestProject"
        hyperionSettings = HyperionSettings(mockProject)
        every { mockProject.getService(HyperionSettings::class.java) } returns hyperionSettings

        mockkConstructor(HyperionSettingsForm.IntervalListPanel::class)
        every { anyConstructed<HyperionSettingsForm.IntervalListPanel>().initPanel() } returns Unit
        mockkStatic(ActionManager::class)
        every { ActionManager.getInstance() } returns mockk()

    }

    @BeforeEach
    fun setup() {
        hyperionSettingsForm = HyperionSettingsForm(mockProject)
        getMethod("createSettings").call(hyperionSettingsForm)
    }

    @Test
    fun `Test getIntervalRows() method`() {
        val intervalMethod = getMethod("getIntervalRows")

        val expectedData = listOf(1, 3600, 3600*24, 3600*24*7)
        hyperionSettings.loadState(HyperionSettings.State().apply { intervals = expectedData })

        // We need the table to be constructed so we call the createUIComponents method
        createTableMethod.call(hyperionSettingsForm)

        intervalMethod.call(hyperionSettingsForm)

        val intervalTableProperty = getProperty("intervalTable")

        val intervalTable: IntervalTable = intervalTableProperty.getter.call(hyperionSettingsForm) as IntervalTable

        for (i in expectedData.indices) {
            assertEquals(expectedData[i], intervalTable.currentData[i].toSeconds())
        }
    }

    private fun getMethod(name: String): KFunction<*> {
        return HyperionSettingsForm::class.memberFunctions.find{it.name == name}
                ?.apply { isAccessible = true }
                ?: throw AssertionError("Could not find HyperionSettingsForm#$name method")
    }

    fun getProperty(name: String): KProperty1<out HyperionSettingsForm, *> {
        return hyperionSettingsForm::class.memberProperties.find { it.name == name }
                ?.apply { isAccessible = true }
                ?: throw AssertionError("Could not find HyperionSettingsForm.$name property")
    }
}
