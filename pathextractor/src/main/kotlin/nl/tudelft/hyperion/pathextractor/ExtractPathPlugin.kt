package nl.tudelft.hyperion.pathextractor

import kotlinx.coroutines.delay
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Class that represents the extractor plugin and extends the abstract pipeline plugin
 */
class ExtractPathPlugin : AbstractPipelinePlugin {
    private var config : Configuration

    constructor(config: Configuration) : super(PipelinePluginConfiguration(config.name, config.redis)) {
        this.config = config
    }

    override suspend fun process(input: String): String? {
        println("From ${Thread.currentThread().name}: $input")
        delay(1000)
        return extractPath(input, config)
    }
}
