@file:JvmName("Main")

package nl.tudelft.hyperion.pathextractor

import nl.tudelft.hyperion.pipeline.runPipelinePlugin

fun main(vararg args: String) {
    runPipelinePlugin(
            args.get(0),
            ::ExtractPathPlugin
    )
}