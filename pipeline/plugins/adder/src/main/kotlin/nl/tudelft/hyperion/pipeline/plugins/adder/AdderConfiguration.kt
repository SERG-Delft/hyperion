package nl.tudelft.hyperion.pipeline.plugins.adder

import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

data class AddConfiguration(
    val key: String,
    val value: String
)

data class AdderConfiguration (
    val plugin: PipelinePluginConfiguration,
    val add: List<AddConfiguration>
)
