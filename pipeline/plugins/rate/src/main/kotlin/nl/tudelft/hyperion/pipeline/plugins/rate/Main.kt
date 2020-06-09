@file:JvmName("Main")

package nl.tudelft.hyperion.pipeline.plugins.rate

import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.pipeline.readYAMLConfig
import nl.tudelft.hyperion.pipeline.runPipelinePlugin
import java.nio.file.Path

/**
 * Use main to launch the rate plugin with the location of the config.yml specified as first parameter.
 * Will execute [runPipelinePlugin] with the path supplied and the [RatePlugin].
 */
fun main(vararg args: String) {
    runBlocking {
        val plugin = readYAMLConfig<RatePlugin>(Path.of(args[0]))
        plugin.queryConnectionInformation()
        joinAll(plugin.run(), plugin.launchReporter())
    }
}
