package nl.tudelft.hyperion.pipeline

import kotlinx.coroutines.Job
import nl.tudelft.hyperion.pipeline.connection.ConfigZMQ
import nl.tudelft.hyperion.pipeline.connection.PipelinePullZMQ
import nl.tudelft.hyperion.pipeline.connection.PipelinePushZMQ

/**
 * Subclass of [AbstractPipelinePlugin] that abstracts some of the logic
 * away for plugins that are purely oriented towards receiving a value,
 * possibly editing it, then sending it onwards to the next step in the
 * pipeline. See the documentation on [process] for information on how
 * to implement such a transformation step.
 */
abstract class TransformingPipelinePlugin(
    config: PipelinePluginConfiguration,
    pmConn: ConfigZMQ = ConfigZMQ(config.pluginManager),
    sink: PipelinePushZMQ = PipelinePushZMQ(),
    source: PipelinePullZMQ = PipelinePullZMQ()
) : AbstractPipelinePlugin(config, pmConn, sink, source) {

    // Ensure that this plugin is passthrough.
    override fun run(): Job {
        if (!isPassthrough) {
            throw IllegalStateException(
                "Cannot run a transforming pipeline plugin if it does not both have a push and a pull."
            )
        }

        return super.run()
    }

    // Handle the message by transforming it.
    @Suppress("TooGenericExceptionCaught")
    override suspend fun onMessageReceived(msg: String) {
        val result = try {
            this.process(msg)
        } catch (ex: Exception) {
            logger.warn(ex) { "Error processing message: '$msg'" }
            null
        } ?: return

        send(result)
    }

    /**
     * Method that performs the plugin transform on the specified input.
     * This method can suspend, thus it can perform asynchronous transformations
     * before returning a result.
     *
     * If this method throws, its result is discarded. Since this will cause
     * data loss, it is recommended to only throw if no other options exist.
     * Returning null will also discard the result, but without the performance
     * cost of throwing/handling an exception.
     */
    abstract suspend fun process(input: String): String?
}
