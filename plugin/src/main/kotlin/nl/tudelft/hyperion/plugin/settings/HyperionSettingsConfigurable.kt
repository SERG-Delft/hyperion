package nl.tudelft.hyperion.plugin.settings

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import nl.tudelft.hyperion.plugin.settings.ui.HyperionSettingsForm
import javax.swing.JComponent

/**
 * Class that represents the Project Configurable for the Hyperion Plugin.
 */
class HyperionSettingsConfigurable(val project: Project) : SearchableConfigurable {
    lateinit var settingsPane: HyperionSettingsForm

    companion object {
        const val ID = "hyperion.settings"
        const val DISPLAY_NAME = "Hyperion"
    }

    /**
     * Unique configurable id for the Hyperion Settings.
     */
    override fun getId(): String {
        return ID
    }

    /**
     * Creates new Swing form that enables user to configure the settings.
     * Usually this method is called on the EDT, so it should not take a long time.
     *
     *
     * @return new Swing form to show, or `null` if it cannot be created
     */
    override fun createComponent(): JComponent? {
        // If settingsPane hasn't been initialized yet we need to create it.
        if (!this::settingsPane.isInitialized) settingsPane = HyperionSettingsForm(project)
        return settingsPane.root
    }

    /**
     * Instructs the settingsPane to reset its values to the last known saved state.
     */
    override fun reset() {
        settingsPane.reset()
    }

    /**
     * Indicates whether the Swing form was modified or not.
     * This method is called very often, so it should not take a long time.
     *
     * @return `true` if the settings were modified, `false` otherwise
     */
    override fun isModified(): Boolean {
        return settingsPane.isModified()
    }

    /**
     * Returns the visible name of the configurable component.
     * Note, that this method must return the display name
     * that is equal to the display name declared in XML
     * to avoid unexpected errors.
     *
     * @return the visible name of the configurable component
     */
    override fun getDisplayName(): String {
        return DISPLAY_NAME
    }

    /**
     * Stores the settings from the Swing form to the configurable component.
     * This method is called on EDT upon user's request.
     */
    override fun apply() {
        settingsPane.apply()
    }
}
