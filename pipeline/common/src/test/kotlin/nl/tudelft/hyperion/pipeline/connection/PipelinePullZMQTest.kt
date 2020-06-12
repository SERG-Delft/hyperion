package nl.tudelft.hyperion.pipeline.connection

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import nl.tudelft.hyperion.pipeline.PeerConnectionInformation
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import org.zeromq.ZMQException

class PipelinePullZMQTest {
    private val host = "tcp://localhost:5000"
    private val socket = mockk<ZMQ.Socket>(relaxed = true)

    @BeforeEach
    internal fun setup() {
        mockkConstructor(ZContext::class)
        every {
            ZContext().createSocket(SocketType.PULL)
        } returns socket
    }

    @AfterEach
    internal fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `on bad message does not crash`() {
        val config = PeerConnectionInformation(host, true)
        val pull = PipelinePullZMQ()

        pull.setupConnection(config)

        every {
            socket.recvStr()
        } throws Exception("Exception goes brrr")

        assertDoesNotThrow { pull.pull() }
    }

    @Test
    fun `setup connection bind`() {
        val config = PeerConnectionInformation(host, true)
        val pull = PipelinePullZMQ()

        pull.setupConnection(config)

        verify {
            socket.bind(host)
        }
    }

    @Test
    fun `setup connection connect`() {
        val config = PeerConnectionInformation(host, false)
        val pull = PipelinePullZMQ()

        pull.setupConnection(config)

        verify {
            socket.connect(host)
        }
    }

    @Test
    fun `pull receives string`() {
        val str = "chicken"
        every {
            socket.recvStr()
        } returns str

        val res = PipelinePullZMQ().pull()

        assertEquals(str, res)
    }

    @Test
    fun `close connection tears down correctly`() {
        PipelinePullZMQ().closeConnection()

        verify {
            socket.close()
            anyConstructed<ZContext>().destroy()
        }
    }
}