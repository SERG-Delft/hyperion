package nl.tudelft.hyperion.pipeline.plugins.stresser

import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Configuration for the stresser pipeline plugin.
 *
 * @param pipeline: General configuration for a pipeline plugin.
 * @param message: The message to be sent
 * @param iterations: The number of messages to be sent. If null, sends endlessly
 */
data class StresserConfiguration(
    val message: String,
    val pipeline: PipelinePluginConfiguration,
    val iterations: Int? = null
)
