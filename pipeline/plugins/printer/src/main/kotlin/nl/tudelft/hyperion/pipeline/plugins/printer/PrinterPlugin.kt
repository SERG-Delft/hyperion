package nl.tudelft.hyperion.pipeline.plugins.printer

import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Pipeline plugin which adds (key, value) pairs to incoming JSON messages.
 * Returns the updated JSON message.
 *
 * @param config: [AdderConfiguration] which specifies default plugin details and which fields to add.
 */
class PrinterPlugin(private var config: PipelinePluginConfiguration) : AbstractPipelinePlugin(config) {
    /**
     * Takes the input string and applies all [AddConfiguration] to it.
     * Expects a json formatted string as input, returns a json formatted string.
     * Uses the given mapper to convert the input string to a tree.
     * Returns the input when string cannot be parsed.
     */
    @Suppress("TooGenericExceptionCaught")
    override suspend fun process(input: String): String {
        println(input)

        return input
    }
}
