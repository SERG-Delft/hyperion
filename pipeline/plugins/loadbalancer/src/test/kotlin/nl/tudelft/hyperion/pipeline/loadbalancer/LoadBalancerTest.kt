package nl.tudelft.hyperion.pipeline.loadbalancer

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.pipeline.PeerConnectionInformation
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
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
    fun `A receiver proxy thread should be created if we have a receiver`() {
        val inst = spyk(
            LoadBalancer(
                LoadBalancerPluginConfiguration(
                    PipelinePluginConfiguration("Balancer", "localhost:3000"),
                    "localhost",
                    3001,
                    3002,
                    3003
                )
            ),
            recordPrivateCalls = true
        )

        every {
            inst["createReceiverProxyThread"]()
        } returns Thread {}

        inst.hasConnectionInformation = true
        inst.subConnectionInformation = PeerConnectionInformation("tcp://localhost:3004", true)
        inst.pubConnectionInformation = PeerConnectionInformation(null, true)

        runBlocking {
            val job = inst.run()
            delay(200)
            job.cancelAndJoin()
        }

        verify {
            inst["createReceiverProxyThread"]()
        }
    }

    @Test
    fun `A sender proxy thread should be created if we have a next stage`() {
        val inst = spyk(
            LoadBalancer(
                LoadBalancerPluginConfiguration(
                    PipelinePluginConfiguration("Balancer", "localhost:3000"),
                    "localhost",
                    3001,
                    3002,
                    3003
                )
            ),
            recordPrivateCalls = true
        )

        every {
            inst["createSenderProxyThread"]()
        } returns Thread {}

        inst.hasConnectionInformation = true
        inst.pubConnectionInformation = PeerConnectionInformation("tcp://localhost:3004", true)
        inst.subConnectionInformation = PeerConnectionInformation(null, true)

        runBlocking {
            val job = inst.run()
            delay(200)
            job.cancelAndJoin()
        }

        verify {
            inst["createSenderProxyThread"]()
        }
    }

    @Test
    fun `Creating a receiver proxy should invoke ZMQ's proxy`() {
        mockkConstructor(ZContext::class)
        mockkStatic(zmq.ZMQ::class)

        val pullMock = mockk<ZMQ.Socket>(relaxed = true)
        val pushMock = mockk<ZMQ.Socket>(relaxed = true)

        every { ZContext().createSocket(SocketType.PULL) } returns pullMock
        every { ZContext().createSocket(SocketType.PUSH) } returns pushMock

        val inst = LoadBalancer(
            LoadBalancerPluginConfiguration(
                PipelinePluginConfiguration("Balancer", "localhost:3000"),
                "localhost",
                3001,
                3002,
                3003
            )
        )

        inst.hasConnectionInformation = true
        inst.subConnectionInformation = PeerConnectionInformation("tcp://localhost:3004", true)
        inst.pubConnectionInformation = PeerConnectionInformation(null, true)

        runBlocking {
            val job = inst.createReceiverProxyThread()
            job.start()
            delay(300)
            job.interrupt()
        }

        verify {
            pullMock.bind("tcp://localhost:3004")
            pushMock.bind("tcp://localhost:3002")
            zmq.ZMQ.proxy(pullMock.base(), pushMock.base(), null)
        }

        unmockkAll()
    }

    @Test
    fun `Creating a sender proxy should invoke ZMQ's proxy`() {
        mockkConstructor(ZContext::class)
        mockkStatic(zmq.ZMQ::class)

        val pullMock = mockk<ZMQ.Socket>(relaxed = true)
        val pushMock = mockk<ZMQ.Socket>(relaxed = true)

        every { ZContext().createSocket(SocketType.PULL) } returns pullMock
        every { ZContext().createSocket(SocketType.PUSH) } returns pushMock

        val inst = LoadBalancer(
            LoadBalancerPluginConfiguration(
                PipelinePluginConfiguration("Balancer", "localhost:3000"),
                "localhost",
                3001,
                3002,
                3003
            )
        )

        inst.hasConnectionInformation = true
        inst.pubConnectionInformation = PeerConnectionInformation("tcp://localhost:3004", true)
        inst.subConnectionInformation = PeerConnectionInformation(null, true)

        runBlocking {
            val job = inst.createSenderProxyThread()
            job.start()
            delay(300)
            job.interrupt()
        }

        verify {
            pullMock.bind("tcp://localhost:3003")
            pushMock.bind("tcp://localhost:3004")
            zmq.ZMQ.proxy(pullMock.base(), pushMock.base(), null)
        }

        unmockkAll()
    }
}
