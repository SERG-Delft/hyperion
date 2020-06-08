package nl.tudelft.hyperion.plugin.settings

import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import nl.tudelft.hyperion.plugin.settings.ui.HyperionSettingsForm
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Test class that tests/verifies all methods from [HyperionSettingsConfigurable].
 */
class HyperionSettingsConfigurableTest {

    private val mockProject: Project = mockk()
    private val mockSettingsForm: HyperionSettingsForm = mockk()
    private lateinit var configurable: HyperionSettingsConfigurable

    @BeforeEach
    fun setup() {
        configurable = HyperionSettingsConfigurable(mockProject).apply { settingsPane = mockSettingsForm }
    }

    @AfterEach
    fun cleanup() {
        unmockkAll()
    }

    @Test
    fun `Test getId`() {
        assertEquals("hyperion.settings", configurable.id)
    }

    @Test
    fun `Test getDisplayName`() {
        assertEquals("Hyperion", configurable.displayName)
    }

    @Test
    fun `Test createComponent`() {
        every { mockSettingsForm.root } returns null

        configurable.createComponent()

        verify(exactly = 1) { mockSettingsForm.root }
    }

    @Test
    fun `Test reset`() {
        every { mockSettingsForm.reset() } just runs

        configurable.reset()

        verify(exactly = 1) { mockSettingsForm.reset() }
    }

    @Test
    fun `Test isModified`() {
        every { mockSettingsForm.isModified } returns false

        configurable.isModified()

        verify(exactly = 1) { mockSettingsForm.isModified }
    }

    @Test
    fun `Test apply`() {
        every { mockSettingsForm.apply() } just runs

        configurable.apply()

        verify(exactly = 1) { mockSettingsForm.apply() }
    }

}
