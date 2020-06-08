package nl.tudelft.hyperion.pipeline.plugins.rate

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class RatePlugin(private val config: RateConfiguration) : AbstractPipelinePlugin(config.pipeline) {
    private val formatter = NumberFormat.getNumberInstance(Locale.US).also {
        it.minimumFractionDigits = 0
        it.maximumFractionDigits = 2
    }

    val throughput = AtomicInteger(0)
    val total = AtomicLong(0)

    override val logger = mu.KotlinLogging.logger {}

    override suspend fun onMessageReceived(msg: String) {
        throughput.incrementAndGet()
        total.incrementAndGet()

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
                ${formatter.format(throughput.get())} messages last ${config.rate} seconds
                ${formatter.format(throughput.get().toDouble() / config.rate)} messages per second
                ${formatter.format(total.get())} messages total
            """.trimIndent()
        }

        throughput.set(0)
    }
}
