package nl.tudelft.hyperion.pipeline.plugins.adder

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin

class AdderPlugin(private var config: AdderConfiguration): AbstractPipelinePlugin(config.plugin) {
    private val mapper = jacksonObjectMapper()

    override suspend fun process(input: String): String? = adder(input, config.add, mapper)
}
