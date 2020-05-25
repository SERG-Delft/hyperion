package nl.tudelft.hyperion.pipeline.connection

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import nl.tudelft.hyperion.pipeline.PeerConnectionInformation
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ

class PipelinePushZMQTest {
    private val host = "tcp://localhost:5000"
    private val socket = mockk<ZMQ.Socket>(relaxed = true)

    @BeforeEach
    internal fun setup() {
        mockkConstructor(ZContext::class)
        every {
            ZContext().createSocket(SocketType.PUSH)
        } returns socket
    }

    @AfterEach
    internal fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `setup connection bind`() {
        val config = PeerConnectionInformation(host, true)
        val pull = PipelinePushZMQ()

        pull.setupConnection(config)

        verify {
            socket.bind(host)
        }
    }

    @Test
    fun `setup connection connect`() {
        val config = PeerConnectionInformation(host, false)
        val pull = PipelinePushZMQ()

        pull.setupConnection(config)

        verify {
            socket.connect(host)
        }
    }

    @Test
    fun `push sends string`() {
        val str = "chicken"
        PipelinePushZMQ().push(str)

        verify {
            socket.send(str, zmq.ZMQ.ZMQ_DONTWAIT)
        }
    }

    @Test
    fun `close connection tears down correctly`() {
        PipelinePushZMQ().closeConnection()

        verify {
            socket.close()
            anyConstructed<ZContext>().destroy()
        }
    }
}