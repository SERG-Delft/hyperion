package nl.tudelft.hyperion.aggregator.intake

import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.aggregator.ZMQConfiguration
import nl.tudelft.hyperion.aggregator.utils.TestWithoutLogging
import nl.tudelft.hyperion.aggregator.workers.AggregationManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ

class ZMQIntakeTest : TestWithoutLogging() {
    @Test
    fun `Setup should query the plugin manager for connection information`() {
        mockkConstructor(ZContext::class)

        val socket = mockk<ZMQ.Socket>(relaxed = true)

        every {
            ZContext().createSocket(SocketType.REQ)
        } returns socket

        every {
            socket.recvStr()
        } returns """
            {"host":"tcp://*:12345","isBind":true}
        """.trimIndent()

        val intake = ZMQIntake(
            ZMQConfiguration("localhost:12346", "Aggregator"),
            mockk(relaxed = true)
        )

        intake.setup()

        verify {
            socket.connect("tcp://localhost:12346")
            socket.send(
                """
                    {"id":"Aggregator","type":"pull"}
                """.trimIndent()
            )
            socket.recvStr()
            socket.close()
        }

        unmockkAll()
    }

    @Test
    fun `Setup should not allow being called twice`() {
        val ctx = ZMQIntake(
            ZMQConfiguration("localhost:12346", "Aggregator"),
            mockk(relaxed = true)
        )

        ctx.setConnectionInformation(PeerConnectionInformation("a", true))

        assertThrows<ZMQIntakeInitializationException> {
            ctx.setup()
        }
    }

    @Test
    fun `Listen should not allow being called without setup`() {
        val ctx = ZMQIntake(
            ZMQConfiguration("localhost:12346", "Aggregator"),
            mockk(relaxed = true)
        )

        assertThrows<ZMQIntakeInitializationException> {
            ctx.listen()
        }
    }

    @Test
    fun `Listen should connect to the queried information - bind`() {
        mockkConstructor(ZContext::class)
        val socket = mockk<ZMQ.Socket>(relaxed = true)

        every {
            ZContext().createSocket(SocketType.PULL)
        } returns socket

        val ctx = ZMQIntake(
            ZMQConfiguration("localhost:12346", "Aggregator"),
            mockk(relaxed = true)
        )
        ctx.setConnectionInformation(PeerConnectionInformation("a", true))

        runBlocking {
            val job = ctx.listen()
            delay(100)
            job.cancelAndJoin()
        }

        verify {
            socket.bind("a")
            socket.close()
        }

        unmockkAll()
    }

    @Test
    fun `Listen should connect to the queried information - connect`() {
        mockkConstructor(ZContext::class)
        val socket = mockk<ZMQ.Socket>(relaxed = true)

        every {
            ZContext().createSocket(SocketType.PULL)
        } returns socket

        val ctx = ZMQIntake(
            ZMQConfiguration("localhost:12346", "Aggregator"),
            mockk(relaxed = true)
        )
        ctx.setConnectionInformation(PeerConnectionInformation("a", false))

        runBlocking {
            val job = ctx.listen()
            delay(100)
            job.cancelAndJoin()
        }

        verify {
            socket.connect("a")
            socket.close()
        }

        unmockkAll()
    }

    @Test
    fun `Listen should call handleMessage on a received message`() {
        mockkConstructor(ZContext::class)
        val socket = mockk<ZMQ.Socket>(relaxed = true)

        every {
            ZContext().createSocket(SocketType.PULL)
        } returns socket

        every {
            socket.recvStr()
        } returns "msg"

        val ctx = spyk(
            ZMQIntake(
                ZMQConfiguration("localhost:12346", "Aggregator"),
                mockk(relaxed = true)
            )
        )
        ctx.setConnectionInformation(PeerConnectionInformation("a", false))

        coJustRun {
            ctx.handleMessage(any())
        }

        runBlocking {
            val job = ctx.listen()
            delay(100)
            job.cancelAndJoin()
        }

        coVerify {
            socket.connect("a")
            socket.close()
            ctx.handleMessage("msg")
        }

        unmockkAll()
    }

    @Test
    fun `Received messages will be aggregated if valid`() {
        val aggregateMock = mockk<AggregationManager>(relaxed = true)

        val intake = ZMQIntake(
            ZMQConfiguration("localhost:12346", "Aggregator"),
            aggregateMock
        )

        runBlocking {
            intake.handleMessage(
                """
                    {
                        "project": "TestProject",
                        "version": "v1.0.0",
                        "severity": "INFO",
                        "location": {
                            "file": "com.test.file",
                            "line": "10"
                        },
                        "timestamp": "2020-05-07T11:22:00.644Z"
                    }
                """.trimIndent()
            )
        }

        coVerify(exactly = 1) {
            aggregateMock.aggregate(any())
        }
    }

    @Test
    fun `Invalid messages will be ignored, without crashes`() {
        val aggregateMock = mockk<AggregationManager>(relaxed = true)

        val intake = ZMQIntake(
            ZMQConfiguration("localhost:12346", "Aggregator"),
            aggregateMock
        )

        // Project is missing
        runBlocking {
            intake.handleMessage(
                """
                    {
                        "version": "v1.0.0",
                        "severity": "INFO",
                        "location": {
                            "file": "com.test.file",
                            "line": "10"
                        },
                        "timestamp": "2020-05-07T11:22:00.644Z"
                    }
                """.trimIndent()
            )
        }

        coVerify(exactly = 0) {
            aggregateMock.aggregate(any())
        }
    }
}
