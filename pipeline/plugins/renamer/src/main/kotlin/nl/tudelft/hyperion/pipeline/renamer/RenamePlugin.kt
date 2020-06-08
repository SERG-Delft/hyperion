package nl.tudelft.hyperion.pipeline.renamer

import nl.tudelft.hyperion.pipeline.TransformingPipelinePlugin

/**
 * Class that extends the AbstractPipelinePlugin class and represents the renamer plugin
 */
class RenamePlugin(private var config: Configuration) : TransformingPipelinePlugin(config.pipeline) {
    override suspend fun process(input: String): String? = rename(input, config)
}
