@file:JvmName("Main")
package nl.tudelft.hyperion.pipeline.sampleplugin

import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import nl.tudelft.hyperion.pipeline.runPipelinePlugin
import kotlinx.coroutines.delay

class SamplePlugin(config: PipelinePluginConfiguration) : AbstractPipelinePlugin(config) {
    override suspend fun process(input: String): String? {
        println("From ${Thread.currentThread().name}: $input")
        delay(1000)
        return input
    }
}

fun main(vararg args: String) = runPipelinePlugin(
    args.firstOrNull() ?: "./config.yaml",
    ::SamplePlugin
)
