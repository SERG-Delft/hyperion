package nl.tudelft.hyperion.pipeline.loadbalancer

import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkConstructor
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.jupiter.api.Test
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ

class LoadBalancerTest {
    @Test
    @ExperimentalCoroutinesApi
    fun `createChannelPass worker should send message from socket to channel`() {
        mockkConstructor(ZContext::class)

        val socket = mockk<ZMQ.Socket>(relaxed = true)

        coEvery {
            ZContext().createSocket(SocketType.REQ)
        } returns socket

        val scope = TestCoroutineScope()
        val channel = Channel<String>()

        val channelPass = scope.createChannelPass(
                "host",
                1111,
                channel,
                SocketType.PUSH
        )
    }
}
