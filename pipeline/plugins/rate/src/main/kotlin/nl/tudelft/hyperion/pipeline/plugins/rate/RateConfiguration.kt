package nl.tudelft.hyperion.pipeline.plugins.rate

import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Configuration for the adder pipeline plugin.
 * @param pipeline: General configuration for a pipeline plugin.
 * @param rate: Plugin will display how many messages were passed through every _rate_ seconds, defaults to 10.
 */
data class RateConfiguration(
    val pipeline: PipelinePluginConfiguration,
    val rate: Int = 10
)
