package nl.tudelft.hyperion.extractor

import nl.tudelft.hyperion.pluginmanager.hyperionplugin.HyperionPlugin
import nl.tudelft.hyperion.pluginmanager.hyperionplugin.PluginConfiguration

/**
 * Class that extends the HyperionPlugin class and represents the extraction plugin
 */
class ExtractPlugin(pluginConfig: PluginConfiguration): HyperionPlugin(pluginConfig) {
    private lateinit var config: Configuration

    constructor(
            config: Configuration
    ) : this(PluginConfiguration(config.redis, config.registrationChannelPostfix, config.name)) { this.config = config }

    /**
     * Function that takes a message string and extracts information from it
     * @param message The message string
     * @return A message containing the additional extracted information
     */
    override fun work(message: String): String {
        return extract(message, config)
    }
}
