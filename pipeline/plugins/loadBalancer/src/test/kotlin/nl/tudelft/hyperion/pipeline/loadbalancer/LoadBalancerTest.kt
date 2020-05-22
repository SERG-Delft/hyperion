package nl.tudelft.hyperion.pipeline.loadbalancer

import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import nl.tudelft.hyperion.pipeline.PeerConnectionInformation
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.net.ServerSocket

class LoadBalancerTest {

    companion object {
        /**
         * Attempts to get an unused port number.
         */
        fun getFreePort(): Int = ServerSocket(0).use { it.localPort }
    }

    /**
     * Represents an object to keep track of used ports.
     */
    private class PortNamespace {
        companion object {
            val usedPorts = mutableSetOf<Int>()
        }

        /**
         * Returns an open port number.
         */
        fun allocatePort(): Int {
            var port = getFreePort()
            while (port in usedPorts) {
                port = getFreePort()
            }
            usedPorts.add(port)
            return port
        }
    }

    private class LowerCasePlugin(id: String, workerManagerPort: Int) : AbstractPipelinePlugin(
            PipelinePluginConfiguration(
                    id,
                    "localhost:$workerManagerPort"
            )
    ) {
        override suspend fun process(input: String): String? {
            return input.toLowerCase()
        }
    }

    @Test
    fun `createChannelReceiver worker should send message from channel to socket`() {
        mockkConstructor(ZContext::class)

        val socket = mockk<ZMQ.Socket>(relaxed = true)

        coEvery {
            ZContext().createSocket(SocketType.PUSH)
        } returns socket

        val scope = CoroutineScope(Dispatchers.Unconfined)
        val channel = Channel<String>()

        val message = "message"
        val hostname = "host"
        val port = 1111

        scope.createChannelReceiver(
                hostname,
                port,
                channel
        )

        runBlocking {
            channel.send(message)
        }

        scope.cancel()

        coVerify(exactly = 1) {
            socket.bind("tcp://$hostname:$port")
            socket.send(message)
        }

        confirmVerified(socket)

        unmockkAll()
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `createChannelSender worker should send to a channel`() {
        mockkConstructor(ZContext::class)

        val socket = mockk<ZMQ.Socket>(relaxed = true)

        coEvery {
            ZContext().createSocket(SocketType.PULL)
        } returns socket

        val scope = CoroutineScope(Dispatchers.Unconfined)
        val channel = Channel<String>()

        val expectedMessage = "message"
        val hostname = "host"
        val port = 1111

        coEvery {
            socket.recvStr()
        } returns expectedMessage

        scope.createChannelSender(
                hostname,
                port,
                channel
        )

        coVerify {
            socket.bind("tcp://$hostname:$port")
            socket.recvStr()
        }

        runBlocking {
            withTimeout(500) {
                assertEquals(expectedMessage, channel.receive())
            }
        }

        scope.cancel()

        unmockkAll()
    }

    @Test
    fun `Messages should pass through entire pipeline`() = runBlocking {
        // TODO maybe use container to run test
        val ns = PortNamespace()

        // Set up load balancer first
        val workerManagerPort = ns.allocatePort()
        val ventPort = ns.allocatePort()
        val sinkPort = ns.allocatePort()

        val config = LoadBalancerPluginConfiguration(
                PipelinePluginConfiguration(
                        "lb",
                        "foo:1234"
                ),
                "localhost",
                workerManagerPort,
                ventPort,
                sinkPort
        )

        val lb = LoadBalancer(config)

        val pubPort = ns.allocatePort()
        lb.pubConnectionInformation = PeerConnectionInformation("tcp://localhost:$pubPort", true)

        val subPort = ns.allocatePort()
        lb.subConnectionInformation = PeerConnectionInformation("tcp://localhost:$subPort", true)

        lb.hasConnectionInformation = true

        val lbJob = lb.run()

        // Next, start 3 plugins aimed at the load balancer
        val mockPlugins = listOf(
                spyk(LowerCasePlugin("mock1", workerManagerPort)),
                spyk(LowerCasePlugin("mock2", workerManagerPort)),
                spyk(LowerCasePlugin("mock3", workerManagerPort))
        )

        val jobs = mockPlugins.map {
            it.queryConnectionInformation()
            it.run()
        }

        // Start an IO coroutine that listens for messages from the load balancer
        val receivedMsgs: MutableList<String> = mutableListOf()
        val receiverJob = CoroutineScope(Dispatchers.IO).launch {
            ZContext().use {
                val sock = it.createSocket(SocketType.PULL)
                sock.receiveTimeOut = 1000
                sock.connect("tcp://localhost:$pubPort")

                while (isActive) {
                    receivedMsgs.add(sock.recvStr())
                }
            }
        }

        // Create a ZeroMQ socket to send messages to the load balancer
        val context = ZContext()
        val sock = context.createSocket(SocketType.PUSH)
        sock.connect("tcp://localhost:$subPort")

        // Queue 3 messages
        for (i in 0..2) {
            sock.send("MESSAGE")
        }

        delay(500L)

        sock.close()
        context.close()

        lbJob.cancel()
        jobs.map { it.cancel() }
        receiverJob.cancel()

        // Check if all 3 messages are processed
        val expected = (0..2).map { "message" }

        // messages should be distributed round-robin
        // so all three plugins should have processed a message
        for (plugin in mockPlugins) {
            coVerify {
                plugin.process("MESSAGE")
            }
        }

        assertEquals(expected, receivedMsgs)
    }
}
