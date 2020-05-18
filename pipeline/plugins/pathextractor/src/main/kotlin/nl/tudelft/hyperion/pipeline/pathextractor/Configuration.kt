package nl.tudelft.hyperion.pipeline.pathextractor

import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Configuration for field extraction
 * @param field the field containing the package name
 * @param relativePathFromSource the path from the source to the package
 * @Param plugin configuration for the abstract plugin
 */
data class Configuration(
        val field : String,
        val relativePathFromSource : String,
        val plugin : PipelinePluginConfiguration
)
