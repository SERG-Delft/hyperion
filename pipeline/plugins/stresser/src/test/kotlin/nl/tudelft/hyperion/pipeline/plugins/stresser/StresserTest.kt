package nl.tudelft.hyperion.pipeline.plugins.stresser

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.pipeline.PeerConnectionInformation
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import nl.tudelft.hyperion.pipeline.connection.PipelinePushZMQ
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StresserTest {
    @Test
    fun `Stresser should check for first step in pipeline`() {
        val plugin = StresserPlugin(
            StresserConfiguration(
                "Msg",
                PipelinePluginConfiguration("printer", "host:3000"),
                1
            )
        )
        plugin.pubConnectionInformation = PeerConnectionInformation("a", false)
        plugin.subConnectionInformation = PeerConnectionInformation("b", false)

        assertThrows<IllegalStateException> {
            plugin.run()
        }
    }

    @Test
    fun `Stresser should publish exact amount of times on bounded`() {
        val plugin = StresserPlugin(
            StresserConfiguration(
                "Message",
                PipelinePluginConfiguration("printer", "host:3000"),
                10
            )
        )

        val push = mockk<PipelinePushZMQ>(relaxed = true)

        runBlocking {
            plugin.run {
                GlobalScope.launch {
                    runBoundedPublisher(push)
                }.join()
            }
        }

        verify(exactly = 10) {
            push.push("Message")
        }
    }

    @Test
    fun `Stresser should publish infinitely when unbounded`() {
        val plugin = StresserPlugin(
            StresserConfiguration(
                "Message",
                PipelinePluginConfiguration("printer", "host:3000")
            )
        )

        val push = mockk<PipelinePushZMQ>(relaxed = true)

        runBlocking {
            plugin.run {
                val job = GlobalScope.launch {
                    runInfinitePublisher(push)
                }

                delay(100)

                job.cancelAndJoin()
            }
        }

        verify(atLeast = 1) {
            push.push("Message")
        }
    }
}
