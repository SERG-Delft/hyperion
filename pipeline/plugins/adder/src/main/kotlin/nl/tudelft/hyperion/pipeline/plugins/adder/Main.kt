@file:JvmName("Main")

package nl.tudelft.hyperion.pipeline.plugins.adder

import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.pipeline.readYAMLConfig
import nl.tudelft.hyperion.pipeline.runPipelinePlugin
import java.nio.file.Path

/**
 * Use main to launch the adder plugin with the location of the config.yml specified as first parameter.
 * Will execute [runPipelinePlugin] with the path supplied and the [AdderPlugin].
 */
fun main(vararg args: String) = run {
    val config = readYAMLConfig<AdderConfiguration>(Path.of(args[0]))
    val plugin = AdderPlugin(config)

    plugin.queryConnectionInformation()
    runBlocking { joinAll(plugin.run(), plugin.launchUpdateConfigFileChanged(args[0])) }
}
