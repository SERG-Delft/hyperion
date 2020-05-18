package nl.tudelft.hyperion.pipeline.extractor

import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

class Extract(val to: String, val type: String)

/**
 * Configuration for renaming plugin
 * @param rename the list of renaming schemes
 */
data class Configuration(
        val plugin: PipelinePluginConfiguration,
        val field: String,
        val match: String,
        val extract: List<Extract>
)
