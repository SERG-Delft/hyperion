package nl.tudelft.hyperion.pipeline.pathextractor

import nl.tudelft.hyperion.pipeline.TransformingPipelinePlugin

/**
 * Class that represents the extractor plugin and extends the abstract pipeline plugin
 */
class ExtractPathPlugin(private var config: Configuration) : TransformingPipelinePlugin(config.pipeline) {
    override suspend fun process(input: String): String? = extractPath(input, config)
}
