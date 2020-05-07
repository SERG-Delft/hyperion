package nl.tudelft.hyperion.renamer

import nl.tudelft.hyperion.pluginmanager.hyperionplugin.HyperionPlugin
import nl.tudelft.hyperion.pluginmanager.hyperionplugin.PluginConfiguration
import java.nio.file.Path

class RenamePlugin(private val config: PluginConfiguration): HyperionPlugin(config) {
    override fun work(message: String): String {
        return rename(message, Configuration.load(Path.of("./renamer/config.yaml").toAbsolutePath()))
    }
}
