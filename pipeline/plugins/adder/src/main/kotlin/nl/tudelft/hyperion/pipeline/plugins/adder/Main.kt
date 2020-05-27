@file:JvmName("Main")

package nl.tudelft.hyperion.pipeline.plugins.adder

import nl.tudelft.hyperion.pipeline.runPipelinePlugin

fun main(vararg args: String) = runPipelinePlugin(
    args[0],
    ::AdderPlugin
)
