package nl.tudelft.hyperion.pipeline.plugins.rate

import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RateConfigurationTest {
    @Test
    fun `default rate is 10`() {
        val config = RateConfiguration(PipelinePluginConfiguration("rate", "localhost"))

        assertEquals(10, config.rate)
    }

    @Test
    fun `non default rate should be 5`() {
        val config = RateConfiguration(PipelinePluginConfiguration("rate", "localhost"), 5)

        assertEquals(5, config.rate)
    }
}