package nl.tudelft.hyperion.pipeline.plugins.rate

import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RatePluginTest {
    private val config = RateConfiguration(PipelinePluginConfiguration("rate", "localhost"))

    @Test
    fun `process should increase throughput count`() {
        val plugin = RatePlugin(config)

        runBlocking { plugin.onMessageReceived("message") }

        assertEquals(1, plugin.throughput.get())
    }

    @Test
    fun `throughput should be 0 on start`() {
        val plugin = RatePlugin(config)

        assertEquals(0, plugin.throughput.get())
    }

    @Test
    fun `report should reset count to 0`() {
        val plugin = RatePlugin(config)

        runBlocking {
            plugin.onMessageReceived("message")
            plugin.report()
        }

        assertEquals(0, plugin.throughput.get())
    }
}
