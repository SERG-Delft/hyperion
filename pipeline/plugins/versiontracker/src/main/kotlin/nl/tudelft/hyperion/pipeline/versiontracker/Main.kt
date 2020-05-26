package nl.tudelft.hyperion.pipeline.versiontracker

import nl.tudelft.hyperion.pipeline.runPipelinePlugin

fun main(vararg args: String) =
    runPipelinePlugin(
        if (args.isNotEmpty()) args[0] else "./config.yml",
        ::VersionTracker
    )
