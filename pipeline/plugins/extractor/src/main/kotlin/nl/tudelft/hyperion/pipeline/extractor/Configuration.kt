package nl.tudelft.hyperion.pipeline.extractor

import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

class Extract(val to: String, val type: String)

/**
 * Configuration for the extraction plugin
 * @param field the name of the field to be matched
 * @param match the regex pattern to match the field on
 * @param extract list of extraction schemes
 */
data class Configuration(
        val plugin: PipelinePluginConfiguration,
        val field: String,
        val match: String,
        val extract: List<Extract>
)
