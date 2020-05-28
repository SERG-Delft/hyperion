package nl.tudelft.hyperion.pipeline.plugins.adder

import com.fasterxml.jackson.databind.ObjectMapper
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin

class AdderPlugin(private var config: AdderConfiguration): AbstractPipelinePlugin(config.pipeline) {
    private val mapper = ObjectMapper()

    override suspend fun process(input: String): String? = adder(input, config.add, mapper)
}
