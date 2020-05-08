package nl.tudelft.hyperion.extractor

import nl.tudelft.hyperion.pluginmanager.hyperionplugin.HyperionPlugin
import nl.tudelft.hyperion.pluginmanager.hyperionplugin.PluginConfiguration

class ExtractPlugin(_pluginConfig: PluginConfiguration): HyperionPlugin(_pluginConfig) {
    private lateinit var config: Configuration

    constructor(
            config: Configuration
    ) : this(PluginConfiguration(config.redis, config.registrationChannelPostfix, config.name)) { this.config = config }

    override fun work(message: String): String {
        return extract(message, config)
    }
}