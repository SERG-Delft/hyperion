package nl.tudelft.hyperion.pipeline.plugins.rate

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import java.util.concurrent.atomic.AtomicInteger

class RatePlugin(private val config: RateConfiguration) : AbstractPipelinePlugin(config.pipeline) {
    var throughput = AtomicInteger(0)
    private val logger = mu.KotlinLogging.logger {}

    override suspend fun process(input: String): String? {
        throughput.incrementAndGet()
        return input
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

    suspend fun report() {
        logger.info {
            """reporting throughput every ${config.rate} seconds
            $throughput messages last ${config.rate} seconds
            ${throughput.get() / config.rate} messages per second.
        """.trimIndent()
        }
        throughput.set(0)
    }
}
