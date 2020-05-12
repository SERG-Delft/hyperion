package nl.tudelft.hyperion.pluginmanager.hyperionplugin

import nl.tudelft.hyperion.pluginmanager.hyperionplugin.PluginConfiguration
import java.time.LocalDateTime

class SamplePlugin(private val config: PluginConfiguration): HyperionPlugin(config) {

    override fun work(message: String): String {
        if (config.name == "Aggregator" && message == "1000") {
            val currentDateTime = LocalDateTime.now()
            println(currentDateTime)
        }
        return message
    }
}