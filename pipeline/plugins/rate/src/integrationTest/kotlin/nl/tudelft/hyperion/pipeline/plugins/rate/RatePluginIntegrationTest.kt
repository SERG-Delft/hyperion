package nl.tudelft.hyperion.pipeline.plugins.rate

import io.mockk.coVerify
import io.mockk.mockkConstructor
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.zeromq.SocketType
import org.zeromq.ZContext
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Executors

class RatePluginIntegrationTest {
    @Test
    fun `run main`() = runBlocking {
        val temporaryFile = File.createTempFile("hyperion-rate-config", "yaml")
        Files.writeString(
            temporaryFile.toPath(), """
                pipeline:
                    manager-host: localhost:39181
                    plugin-id: rate
                rate: 2
            """.trimIndent()
        )
        dummyPluginManager(39181)
        mockkConstructor(RatePlugin::class)
        val plugin = Thread(RatePluginRunnable(temporaryFile.absolutePath))
        plugin.start()
        
        val (pusher, channel) = runDummyZMQPublisher(39182);
        for (i in 1..10) {
            channel.send("message")
        }
        Thread.sleep(4000)
        verify {
            anyConstructed<RatePlugin>().launchReporter()
        }
        coVerify(exactly = 10) {
            anyConstructed<RatePlugin>().onMessageReceived("message")
        }

    }

    /**
     * Helper function that starts a new ZMQ plugin server that always returns the
     * same content for every request.
     */
    private fun dummyPluginManager(port: Int) = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    ).launch {
        val ctx = ZContext()
        val sock = ctx.createSocket(SocketType.REP)
        sock.bind("tcp://*:$port")

        sock.recvStr()
        sock.send("""{"host":"tcp://localhost:39182","isBind":false}""")
        sock.recvStr()
        sock.send("""{"host":"tcp://localhost:39183","isBind":true}""")

        sock.close()
        ctx.destroy()
    }

    /**
     * Helper function that creates a new ZMQ pusher. Returns the job (for
     * cancelling) and a channel that can be used to publish messages.
     */
    private fun runDummyZMQPublisher(port: Int): Pair<Job, Channel<String>> {
        val channel = Channel<String>()

        return Pair(
            CoroutineScope(
                Executors.newSingleThreadExecutor().asCoroutineDispatcher()
            ).launch {
                val ctx = ZContext()
                val sock = ctx.createSocket(SocketType.PUSH)
                sock.bind("tcp://*:$port")

                while (isActive) {
                    sock.send(channel.receive())
                }

                sock.close()
                ctx.destroy()
            }, channel
        )
    }
}

class RatePluginRunnable(private val path: String) : Runnable {

    override fun run() {
        main(path)
    }
}
