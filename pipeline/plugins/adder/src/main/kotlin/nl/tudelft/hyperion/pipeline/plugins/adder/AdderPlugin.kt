package nl.tudelft.hyperion.pipeline.plugins.adder

import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin

class AdderPlugin(private var config: AdderConfiguration): AbstractPipelinePlugin(config.plugin) {
    override suspend fun process(input: String): String? = adder(input, config.add)
}
