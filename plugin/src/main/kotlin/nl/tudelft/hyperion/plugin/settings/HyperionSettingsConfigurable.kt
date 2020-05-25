package nl.tudelft.hyperion.plugin.settings

import com.intellij.openapi.options.NonDefaultProjectConfigurable
import com.intellij.openapi.options.SearchableConfigurable
import nl.tudelft.hyperion.plugin.settings.ui.HyperionSettingsForm
import javax.swing.JComponent

class HyperionSettingsConfigurable : SearchableConfigurable, NonDefaultProjectConfigurable {
    private val settingsPane: HyperionSettingsForm = HyperionSettingsForm()


    /**
     * Unique configurable id for the Hyperion Settings.
     */
    override fun getId(): String {
        return "hyperion.settings"
    }

    /**
     * Creates new Swing form that enables user to configure the settings.
     * Usually this method is called on the EDT, so it should not take a long time.
     *
     *
     * @return new Swing form to show, or `null` if it cannot be created
     */
    override fun createComponent(): JComponent? {
        return settingsPane.root
    }

    /**
     * Indicates whether the Swing form was modified or not.
     * This method is called very often, so it should not take a long time.
     *
     * @return `true` if the settings were modified, `false` otherwise
     */
    override fun isModified(): Boolean {
        return false;
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
        return "HyperionSettings"
    }

    /**
     * Stores the settings from the Swing form to the configurable component.
     * This method is called on EDT upon user's request.
     *
     * @throws ConfigurationException if values cannot be applied
     */
    override fun apply() {

    }

}