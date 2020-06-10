package nl.tudelft.hyperion.pipeline.plugins.adder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.zeromq.SocketType
import org.zeromq.ZContext
import java.io.File
import java.nio.file.Files
import java.util.concurrent.Executors

class AdderPluginIntegrationTest {
    @Test
    fun `config file update has live effect`() = runBlocking {
        val temporaryFile = File.createTempFile("hyperion-add-config", "yaml")
        Files.writeString(
            temporaryFile.toPath(), """
                pipeline:
                    manager-host: localhost:39181
                    plugin-id: rate
                add:
                    - key: version
                      value: 1
            """.trimIndent()
        )
        dummyPluginManager(39181)
        val plugin = Thread(AdderPluginRunnable(temporaryFile.absolutePath))
        plugin.start()

        val recvData = mutableListOf<String>()
        val sink = Thread(SinkRunnable("tcp://localhost:39183", recvData))
        sink.start()
        Thread.sleep(2000)

        val (pusher, channel) = runDummyZMQPublisher(39182);
        channel.send("{}")
        Thread.sleep(1000)


        // update configuration file
        Files.writeString(
            temporaryFile.toPath(), """
                pipeline:
                    manager-host: localhost:39181
                    plugin-id: rate
                add:
                    - key: version
                      value: 2
            """.trimIndent()
        )
        Thread.sleep(1000)

        channel.send("{}")
        Thread.sleep(1000)

        assertEquals(listOf("""{"version":"2"}""", """{"version":"1"}"""), recvData)
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

class AdderPluginRunnable(private val path: String) : Runnable {

    override fun run() {
        main(path)
    }
}

class SinkRunnable(
    private val host: String,
    var recvData: MutableList<String>
) : Runnable {
    override fun run() {
        ZContext().use {
            val puller = it.createSocket(SocketType.PULL)
            puller.connect(host)
            for (msgCount in 0..2) {
                val msg = puller.recvStr()
                recvData.add(0, msg)
            }
        }
    }
}