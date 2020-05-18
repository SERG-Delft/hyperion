package nl.tudelft.hyperion.pipeline.pathextractor

import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Configuration for field extraction
 */
data class Configuration(
        val field : String,
        val relativePathFromSource : String,
        val plugin : PipelinePluginConfiguration
)
