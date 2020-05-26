@file:JvmName("Main")

package nl.tudelft.hyperion.pipeline.extractor

import nl.tudelft.hyperion.pipeline.runPipelinePlugin

fun main(vararg args: String) {
    runPipelinePlugin(
            args.get(0),
            ::ExtractPlugin
    )
}
