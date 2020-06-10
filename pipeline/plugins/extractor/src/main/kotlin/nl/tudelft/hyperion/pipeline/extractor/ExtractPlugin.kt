package nl.tudelft.hyperion.pipeline.extractor

import nl.tudelft.hyperion.pipeline.TransformingPipelinePlugin

/**
 * Class that represents the extractor plugin and extends the abstract pipeline plugin
 */
class ExtractPlugin(private var config: Configuration) : TransformingPipelinePlugin(config.pipeline) {
    override suspend fun process(input: String): String? = extract(input, config)
}
