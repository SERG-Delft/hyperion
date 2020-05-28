package nl.tudelft.hyperion.pipeline.renamer

import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

class Rename(val from: String, val to: String)

/**
 * Configuration for renaming plugin
 * @param rename the list of renaming schemes
 */
data class Configuration(
    val rename: List<Rename>,
    val pipeline: PipelinePluginConfiguration
)
