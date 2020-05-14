@file:JvmName("Main")

package nl.tudelft.hyperion.pipeline.renamer

import nl.tudelft.hyperion.pipeline.runPipelinePlugin

fun main(vararg args: String) = runPipelinePlugin(
    args[0],
    ::RenamePlugin
)
