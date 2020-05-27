package nl.tudelft.hyperion.pipeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PipelinePluginConfigurationTest {
    @Test
    fun `bufferSize default to 20k`() {
        val id = "Plugin"
        val pluginManager = "tcp://localhost:5000"

        val config = PipelinePluginConfiguration(id, pluginManager)

        assertEquals(20_000, config.bufferSize)
    }
}