package nl.tudelft.hyperion.pipeline.plugins.printer

import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PluginTest {
    @Test
    fun `Debug plugin should return input`() {
        val plugin = PrinterPlugin(PipelinePluginConfiguration("printer", "host:3000"))
        runBlocking{
            val output = plugin.process("input")
            Assertions.assertEquals(output, "input")
        }
    }
}