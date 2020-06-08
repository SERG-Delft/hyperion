package nl.tudelft.hyperion.pipeline.plugins.reader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import kotlin.coroutines.CoroutineContext

/**
 * Pipeline plugin which simply prints incoming messages and forwards them
 *
 * @param config: [PipelinePluginConfiguration] configuration for the abstract plugin.
 */
class ReaderPlugin(private var config: PipelinePluginConfiguration) : AbstractPipelinePlugin(config) {
    override val logger = mu.KotlinLogging.logger {}

    // Verify that we're the first step in the pipeline.
    override fun run(context: CoroutineContext): Job {
        if (!canSend || canReceive) {
            throw IllegalStateException("The 'reader' plugin must be the first step in the pipeline.")
        }

        return CoroutineScope(Dispatchers.Default).launch {
            val mainJob = super.run(context)
            val readLoop = runReadLoop()

            // Sleep while workers are active
            try {
                mainJob.join()
            } finally {
                // cancel and cleanup
                mainJob.cancelAndJoin()
                readLoop.cancelAndJoin()
            }
        }
    }

    override suspend fun onMessageReceived(msg: String) {
        // Should never happen
    }

    /**
     * Starts an infinite loop that reads input from stdin and publishes
     * it to the next steps in the pipeline.
     */
    fun runReadLoop() = GlobalScope.launch {
        while (isActive) {
            val line = readLineSuspending() ?: break

            logger.debug { "Publishing line: '$line'" }
            send(line)
        }
    }
}

/**
 * Wrapper around [readLine] that allows for use within a coroutine.
 */
suspend fun readLineSuspending() = withContext(Dispatchers.IO) {
    readLine()
}
