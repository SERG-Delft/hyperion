@file:JvmName("Main")

package nl.tudelft.hyperion.pipeline.plugins.adder

import nl.tudelft.hyperion.pipeline.runPipelinePlugin

/**
 * Use main to launch the adder plugin with the location of the config.yml specified as first parameter.
 * Will execute [runPipelinePlugin] with the path supplied and the [AdderPlugin].
 */
fun main(vararg args: String) = runPipelinePlugin(
    args[0],
    ::AdderPlugin
)
