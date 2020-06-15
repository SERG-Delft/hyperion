package nl.tudelft.hyperion.plugin.settings

import com.intellij.ide.ui.search.SearchableOptionContributor
import com.intellij.ide.ui.search.SearchableOptionProcessor

/**
 * Class that adds the custom search options for the Settings page.
 * See [HyperionSettings] and [nl.tudelft.hyperion.plugin.settings.ui.HyperionSettingsForm]
 */
class HyperionOptionContributor : SearchableOptionContributor() {

    override fun processOptions(processor: SearchableOptionProcessor) {
        processOptions("API Address:", "API Address", processor)
        processOptions("Project:", "Project", processor)
        processOptions("Displayed metric intervals:", "Metric intervals", processor)
    }

    private fun processOptions(text: String, hit: String, processor: SearchableOptionProcessor) {
        processor.addOptions(
            text, null, hit,
            HyperionSettingsConfigurable.ID, HyperionSettingsConfigurable.DISPLAY_NAME, true
        )
    }
}
