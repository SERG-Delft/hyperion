package nl.tudelft.hyperion.renamer

import nl.tudelft.hyperion.pluginmanager.hyperionplugin.HyperionPlugin
import nl.tudelft.hyperion.pluginmanager.hyperionplugin.PluginConfiguration
import java.nio.file.Path

class RenamePlugin(_pluginConfig: PluginConfiguration): HyperionPlugin(_pluginConfig) {
    private lateinit var config: Configuration

    constructor(
            config: Configuration
    ) : this(PluginConfiguration(config.redis, config.registrationChannelPostfix, config.name)) { this.config = config }

    override fun work(message: String): String {
        return rename(message, config)
    }
}