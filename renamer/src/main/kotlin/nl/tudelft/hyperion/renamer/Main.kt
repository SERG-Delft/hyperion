@file:JvmName("Main")

package nl.tudelft.hyperion.renamer

import nl.tudelft.hyperion.pipeline.runPipelinePlugin


fun main(vararg args: String) = runPipelinePlugin(
        args.firstOrNull() ?: "./renamer/config.yaml",
        ::RenamePlugin
)
