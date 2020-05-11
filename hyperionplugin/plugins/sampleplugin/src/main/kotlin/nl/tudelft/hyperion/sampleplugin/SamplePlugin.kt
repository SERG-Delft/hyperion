package nl.tudelft.hyperion.hyperionplugin.plugins.sampleplugin

import nl.tudelft.hyperion.pluginmanager.hyperionplugin.HyperionPlugin
import nl.tudelft.hyperion.pluginmanager.hyperionplugin.PluginConfiguration

class SamplePlugin(private val config: PluginConfiguration): HyperionPlugin(config) {

    override fun work(message: String): String {
        return "${config.name}: [$message]"
    }
}
