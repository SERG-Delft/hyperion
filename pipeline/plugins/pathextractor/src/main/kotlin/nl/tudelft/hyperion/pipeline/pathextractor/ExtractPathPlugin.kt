package nl.tudelft.hyperion.pipeline.pathextractor

import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin

/**
 * Class that represents the extractor plugin and extends the abstract pipeline plugin
 */
class ExtractPathPlugin(private var config: Configuration): AbstractPipelinePlugin(config.plugin) {
    override suspend fun process(input: String): String? = extractPath(input, config)
}
