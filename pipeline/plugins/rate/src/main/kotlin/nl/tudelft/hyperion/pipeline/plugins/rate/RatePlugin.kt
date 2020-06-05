package nl.tudelft.hyperion.pipeline.plugins.rate

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import java.util.concurrent.atomic.AtomicInteger

class RatePlugin(private val config: RateConfiguration) : AbstractPipelinePlugin(config.pipeline) {
    var throughput = AtomicInteger(0)
    override val logger = mu.KotlinLogging.logger {}

    override suspend fun onMessageReceived(msg: String) {
        throughput.incrementAndGet()

        if (canSend) {
            send(msg)
        }
    }

    @Suppress("MagicNumber")
    fun launchReporter() = GlobalScope.launch {
        logger.info { "Rate plugin reporter launched" }

        while (isActive) {
            report()
            delay(1000 * config.rate.toLong())
        }

        logger.info { "Rate plugin reporter closed" }
    }

    fun report() {
        logger.info {
            """reporting throughput every ${config.rate} seconds
            $throughput messages last ${config.rate} seconds
            ${throughput.get() / config.rate} messages per second.
        """.trimIndent()
        }
        throughput.set(0)
    }
}
