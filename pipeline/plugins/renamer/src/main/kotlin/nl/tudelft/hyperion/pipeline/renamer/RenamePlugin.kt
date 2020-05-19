package nl.tudelft.hyperion.pipeline.renamer

import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin

/**
 * Class that extends the AbstractPipelinePlugin class and represents the renamer plugin
 */
class RenamePlugin(private var config: Configuration): AbstractPipelinePlugin(config.plugin) {
    override suspend fun process(input: String): String? = rename(input, config)
}
