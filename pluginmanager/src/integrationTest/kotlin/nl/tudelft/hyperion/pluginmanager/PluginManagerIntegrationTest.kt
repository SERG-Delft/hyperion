package nl.tudelft.hyperion.pluginmanager

import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.Runnable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.zeromq.SocketType
import org.zeromq.ZMQ

class PluginManagerIntegrationTest {
    val host = "tcp://localhost:5560"

    /**
     * This test launches the pluginmanager from main, startsup the loop and
     * replicates plugins sending register requests.
     */
    @Test
    fun `integrationTest PluginManager, registering modules`() {
        // setup pluginmanager
        val tpm = Thread(PluginManagerRunnable(host))
        tpm.start()

        // run a zmq client in a different thread which sends the register requests
        // valid requests
        zmqRequest(
            "Datasource", "push",
            """{"host":"tcp://localhost:1200","isBind":true}"""
        )
        zmqRequest(
            "Renamer", "push",
            """{"host":"tcp://localhost:1201","isBind":true}"""
        )
        zmqRequest(
            "Renamer", "pull",
            """{"host":"tcp://localhost:1200","isBind":false}"""
        )
        zmqRequest(
            "Aggregator", "pull",
            """{"host":"tcp://localhost:1201","isBind":false}"""
        )
        zmqRequest(
            "Datasource", "pull",
            """{"host":null,"isBind":false}"""
        )

        // invalid requests
        zmqRequest("Chicken", "push", "Invalid Request")
        zmqRequest("Renamer", "drop", "Invalid Request")
    }

    private fun zmqRequest(pluginName: String, type: String, reply: String) {
        val req = """{"id": "$pluginName", "type": "$type"}"""

        // connect ZMQ socket
        val context = ZMQ.context(1)
        val requester = context.socket(SocketType.REQ)
        requester.connect(host)

        // send request and verify reply
        var recv = false
        requester.send(req)
        while (!recv) {
            val rep = requester.recvStr(0)
            recv = true
            assertEquals(reply, rep)
        }
        requester.close()
        context.term()
    }
}

class PluginManagerRunnable(private val host: String) : Runnable {

    override fun run() {
        println("${Thread.currentThread()} has run.")
        loadPluginManagerHandler(host)
    }

    private fun loadPluginManagerHandler(host: String) {
        val plugins = listOf(
            PipelinePluginConfig("Datasource", "tcp://localhost:1200"),
            PipelinePluginConfig("Renamer", "tcp://localhost:1201"),
            PipelinePluginConfig("Aggregator", "tcp://localhost:1202")
        )
        val config = Configuration(host, plugins)

        mockkObject(Configuration.Companion)
        every { Configuration.load(any()) } returns config

        main("chicken")
    }
}
