package nl.tudelft.hyperion.pipeline.renamer

import kotlinx.coroutines.delay
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Class that extends the AbstractPipelinePlugin class and represents the renamer plugin
 */
class RenamePlugin : AbstractPipelinePlugin {
    private var config : Configuration

    constructor(config: Configuration) : super(PipelinePluginConfiguration(config.name, config.redis)) {
        this.config = config
    }

    override suspend fun process(input: String): String? {
        println("From ${Thread.currentThread().name}: $input")
        delay(1000)
        return rename(input, config)
    }
}
