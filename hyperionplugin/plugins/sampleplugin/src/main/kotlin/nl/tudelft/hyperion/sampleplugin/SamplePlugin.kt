package nl.tudelft.hyperion.hyperionplugin.plugins.sampleplugin

import nl.tudelft.hyperion.hyperionplugin.common.HyperionPlugin
import nl.tudelft.hyperion.hyperionplugin.common.PluginConfiguration

class SamplePlugin(private val config: PluginConfiguration): HyperionPlugin(config) {

    override fun work(message: String): String {
        return "${config.name}: [$message]"
    }
}
