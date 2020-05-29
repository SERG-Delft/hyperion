package nl.tudelft.hyperion.plugin.settings

import com.intellij.openapi.project.Project
import io.mockk.mockk
import nl.tudelft.hyperion.plugin.settings.HyperionSettings
import nl.tudelft.hyperion.plugin.settings.HyperionSettingsConfigurable
import nl.tudelft.hyperion.plugin.settings.ui.HyperionSettingsForm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class HyperionSettingsTest {

    private val project = mockk<Project>()
    private lateinit var configurable: HyperionSettingsConfigurable
    private lateinit var settingsForm: HyperionSettingsForm

    @BeforeEach
    fun setup() {
        configurable = HyperionSettingsConfigurable(project)
        val settingsPaneField = configurable::class.memberProperties.find{it.name == "settingsPane"}
        settingsPaneField?.let {
            it.isAccessible = true
            settingsForm = it.getter.call(configurable) as HyperionSettingsForm
        }
    }

    @Test
    fun `Test getId()`() {
        assertEquals("hyperion.settings", configurable.id)
    }

}