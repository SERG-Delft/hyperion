package nl.tudelft.hyperion.pluginmanager

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class MainTest() {
    @Test
    fun `Start PluginManager when right config`() {
        val host = "tcp://localhost:5560"
        val plugins = listOf(
            PipelinePluginConfig("Datasource", "tcp://localhost:1200"),
            PipelinePluginConfig("Renamer", "tcp://localhost:1201"),
            PipelinePluginConfig("Aggregator", "tcp://localhost:1202")
        )
        val config = Configuration(host, plugins)

        mockkObject(Configuration.Companion)
        every { Configuration.load(any()) } returns config

        mockkConstructor(PluginManager::class)
        every { anyConstructed<PluginManager>().launchListener() } returns Unit

        main("chicken")

        verify { anyConstructed<PluginManager>().launchListener() }
    }

    @AfterEach
    internal fun tearDown() {
        clearAllMocks()
    }
}
