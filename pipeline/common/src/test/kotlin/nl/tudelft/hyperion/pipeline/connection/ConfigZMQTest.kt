package nl.tudelft.hyperion.pipeline.connection

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ

class ConfigZMQTest {
    @Test
    fun `request config returns received config`() {
        mockkConstructor(ZContext::class)
        val socket = mockk<ZMQ.Socket>(relaxed = true)
        every {
            ZContext().createSocket(SocketType.REQ)
        } returns socket

        every {
            socket.recvStr()
        } returns """{"isBind":"true","host":"tcp://localhost:1200"}"""

        val host = "localhost:5000"
        val config = ConfigZMQ(host)

        val res = config.requestConfig("Renamer", ConfigType.PULL)
        val expected = """{"isBind":"true","host":"tcp://localhost:1200"}"""

        assertEquals(expected, res)

        clearAllMocks()
    }
}