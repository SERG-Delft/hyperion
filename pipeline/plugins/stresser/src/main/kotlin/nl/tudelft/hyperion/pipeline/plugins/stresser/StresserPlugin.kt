package nl.tudelft.hyperion.pipeline.plugins.stresser

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import nl.tudelft.hyperion.pipeline.connection.PipelinePushZMQ
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

/**
 * Pipeline plugin which sends a specified message as fast as possible to the
 * next stage in the pipeline.
 *
 * @param config: [PipelinePluginConfiguration] configuration for the abstract plugin.
 */
class StresserPlugin(private val config: StresserConfiguration) : AbstractPipelinePlugin(config.pipeline) {
    private val workerScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    override val logger = mu.KotlinLogging.logger {}

    // Verify that we're the first step in the pipeline.
    override fun run(context: CoroutineContext): Job {
        if (!canSend || canReceive) {
            throw IllegalStateException("The 'stresser' plugin must be the first step in the pipeline.")
        }

        return workerScope.launch {
            // Set up our own pipeline pusher on this thread so we are not limited by the channel capacity.
            val push = PipelinePushZMQ()
            push.setupConnection(pubConnectionInformation)

            if (config.iterations != null) {
                runBoundedPublisher(push)
            } else {
                runInfinitePublisher(push)
            }

            push.closeConnection()
        }
    }

    override suspend fun onMessageReceived(msg: String) {
        // Should never happen
    }

    /**
     * Blocking action that will run a bounded publisher that
     * publishes the configured message the configured number
     * of times.
     */
    fun CoroutineScope.runBoundedPublisher(push: PipelinePushZMQ) {
        logger.info { "Will publish configured message ${config.iterations} times..." }

        for (i in 1..config.iterations!!) {
            if (!isActive) {
                break
            }

            push.push(config.message)
        }

        logger.info { "All done. Quitting." }
    }

    /**
     * Blocking action that will keep publishing the specified message
     * until this job is cancelled or the process is killed.
     */
    fun CoroutineScope.runInfinitePublisher(push: PipelinePushZMQ) {
        var numSent = 0L

        Runtime.getRuntime().addShutdownHook(Thread {
            logger.info { "Shutting down. Sent $numSent messages total." }
        })

        logger.info { "Will infinitely publish specified message. ^C to quit." }

        while (isActive) {
            numSent += 1
            push.push(config.message)
        }
    }
}
