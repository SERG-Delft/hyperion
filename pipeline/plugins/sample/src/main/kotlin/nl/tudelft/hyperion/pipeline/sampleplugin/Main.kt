@file:JvmName("Main")

package nl.tudelft.hyperion.pipeline.sampleplugin

import kotlinx.coroutines.delay
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import nl.tudelft.hyperion.pipeline.runPipelinePlugin

class SamplePlugin(config: PipelinePluginConfiguration) : AbstractPipelinePlugin(config) {
    override suspend fun process(input: String): String? {
        delay(1000)
        return input
    }
}

fun main(vararg args: String) = runPipelinePlugin(
    args.firstOrNull() ?: "./sample-plugin.yaml",
    ::SamplePlugin
)
