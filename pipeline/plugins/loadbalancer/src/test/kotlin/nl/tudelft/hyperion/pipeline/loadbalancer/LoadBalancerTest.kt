package nl.tudelft.hyperion.pipeline.loadbalancer

import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.util.concurrent.Executors

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoadBalancerTest {
    @BeforeAll
    fun setUp() {
        // reset managerScope
        WorkerManager.managerScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
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
}
