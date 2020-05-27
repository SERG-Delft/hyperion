package nl.tudelft.hyperion.pipeline

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ

class AbstractPluginIntegrationTest {
    private val pluginId = "TestPlugin"

    private val pluginManagerHost = "localhost:5000"
    private val pluginPull = "tcp://localhost:5001"
    private val pluginPush = "tcp://localhost:5002"

    @Test
    fun `Request configuration from PluginManager`() {
        val requests = mutableListOf<String>()
        val recvData = mutableListOf<String>()

        // setup dummy PluginManager
        val pluginManager = Thread(
            PluginManagerRunnable(pluginManagerHost, pluginPull, pluginPush, pluginId, requests)
        )
        pluginManager.start()

        // setup test plugin
        val plugin = setupPlugin()

        // assert request config from :PluginManager:
        plugin.queryConnectionInformation()
        assertEquals(
            listOf(
                """{"id":"$pluginId","type":"push"}""",
                """{"id":"$pluginId","type":"pull"}"""
            ), requests
        )

        // stop pluginmanager as it is no longer needed
        pluginManager.interrupt()

        // push data to the plugin
        plugin.run()
        val sink = Thread(SinkRunnable(pluginPush, recvData))
        sink.start()
        Thread.sleep(200)

        pushData(pluginPull, "testMessage")
        pushData(pluginPull, "message2")

        // assert pushed data is recveid, processed and pused to pull port.
        assertEquals(listOf("message2", "testMessage"), recvData)
    }

    private fun setupPlugin(): TestPlugin {
        val config = PipelinePluginConfiguration(pluginId, pluginManagerHost)
        return TestPlugin(config)
    }

    private fun pushData(host: String, data: String) {
        val context = ZMQ.context(1)
        val pusher = context.socket(SocketType.PUSH)

        pusher.bind(host)
        pusher.send(data)

        pusher.close()
        context.term()
    }
}

class TestPlugin(config: PipelinePluginConfiguration) : AbstractPipelinePlugin(config) {
    override suspend fun process(input: String): String? {
        return input
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
            for (msgCount in  0..2) {
                val msg = puller.recvStr()
                recvData.add(0, msg)
            }
        }
    }
}

class PluginManagerRunnable(
    private val host: String,
    private val pluginPull: String,
    private val pluginPush: String,
    private val pluginId: String,
    var requests: MutableList<String>
) : Runnable {

    override fun run() {

        ZContext().use {
            val socket = it.createSocket(SocketType.REP)
            socket.bind("tcp://$host")
            for (msgCount in  0..2) {
                val msg = socket.recvStr()
                requests.add(0, msg)
                val rep = when (msg) {
                    """{"id":"$pluginId","type":"pull"}""" -> {
                        """{"isBind":"false","host":"$pluginPull"}"""
                    }
                    """{"id":"$pluginId","type":"push"}""" -> {
                        """{"isBind":"true","host":"$pluginPush"}"""
                    }
                    else -> "Invalid Request"
                }
                socket.send(rep)
            }
        }
    }
}
