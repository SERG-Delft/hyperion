package nl.tudelft.hyperion.pipeline.plugins.reader

import io.mockk.justRun
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.pipeline.PeerConnectionInformation
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.lang.IllegalStateException

class ReaderTest {
    @Test
    fun `Reader should check for first step in pipeline`() {
        val plugin = ReaderPlugin(PipelinePluginConfiguration("printer", "host:3000"))
        plugin.pubConnectionInformation = PeerConnectionInformation("a", false)
        plugin.subConnectionInformation = PeerConnectionInformation("b", false)

        assertThrows<IllegalStateException> {
            plugin.run()
        }
    }

    @Test
    fun `Reader should read from stdin and publish the results`() {
        val plugin = spyk(
            ReaderPlugin(PipelinePluginConfiguration("printer", "host:3000")),
            recordPrivateCalls = true
        )

        justRun {
            plugin["send"](any<String>())
        }

        // Mock stdin.
        val sysInBackup = System.`in` // backup System.in to restore it later
        val `in` = ByteArrayInputStream("First message\nSecond message\n\nThird message\n".toByteArray())
        System.setIn(`in`)

        val job = plugin.runReadLoop()

        runBlocking {
            delay(100)
            job.cancelAndJoin()
        }

        verify {
            plugin["send"]("First message")
            plugin["send"]("Second message")
            plugin["send"]("")
            plugin["send"]("Third message")
        }

        System.setIn(sysInBackup)
    }
}
