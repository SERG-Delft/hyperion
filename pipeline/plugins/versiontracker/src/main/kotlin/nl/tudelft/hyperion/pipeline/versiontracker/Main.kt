package nl.tudelft.hyperion.pipeline.versiontracker

import nl.tudelft.hyperion.pipeline.runPipelinePlugin

fun main(vararg args: String) =
        runPipelinePlugin(
                args[0],
                ::VersionTracker
        )
