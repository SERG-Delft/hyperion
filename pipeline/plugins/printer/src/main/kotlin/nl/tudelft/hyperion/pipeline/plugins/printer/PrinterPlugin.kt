package nl.tudelft.hyperion.pipeline.plugins.printer

import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Pipeline plugin which simply prints incoming messages and forwards them
 *
 * @param config: [PipelinePluginConfiguration] configuration for the abstract plugin.
 */
class PrinterPlugin(private var config: PipelinePluginConfiguration) : AbstractPipelinePlugin(config) {
    private val logger = mu.KotlinLogging.logger {}

    /**
     * Takes the input string and prints it.
     * Returns the input.
     */
    override suspend fun process(input: String): String {
        logger.info(input)

        return input
    }
}