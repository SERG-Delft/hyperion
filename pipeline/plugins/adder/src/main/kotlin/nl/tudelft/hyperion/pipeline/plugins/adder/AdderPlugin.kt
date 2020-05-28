package nl.tudelft.hyperion.pipeline.plugins.adder

import com.fasterxml.jackson.databind.ObjectMapper
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin

/**
 * Pipeline plugin which adds (key, value) pairs to incoming JSON messages.
 * Returns the updated JSON message.
 *
 * @param config: [AdderConfiguration] which specifies default plugin details and which fields to add.
 */
class AdderPlugin(private var config: AdderConfiguration): AbstractPipelinePlugin(config.pipeline) {
    private val mapper = ObjectMapper()

    override suspend fun process(input: String): String? = adder(input, config.add, mapper)
}
