package nl.tudelft.hyperion.pipeline.plugins.adder

import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

data class AddConfiguration(
    val key: Any,
    val value: Any,
    val location: String? = null
)

data class AdderConfiguration (
    val plugin: PipelinePluginConfiguration,
    val add: List<AddConfiguration>
)
