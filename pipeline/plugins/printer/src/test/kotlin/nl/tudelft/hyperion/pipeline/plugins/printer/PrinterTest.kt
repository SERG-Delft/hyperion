package nl.tudelft.hyperion.pipeline.plugins.printer

import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PrinterTest {
    @Test
    fun `Debug plugin should return input`() {
        val plugin = PrinterPlugin(PipelinePluginConfiguration("printer", "host:3000"))

        runBlocking {
            plugin.onMessageReceived("input")
        }
    }
}
