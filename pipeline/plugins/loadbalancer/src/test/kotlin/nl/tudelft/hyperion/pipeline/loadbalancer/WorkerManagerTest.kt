package nl.tudelft.hyperion.pipeline.loadbalancer

import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import nl.tudelft.hyperion.pipeline.readJSONContent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.util.concurrent.Executors

class WorkerManagerTest {

    lateinit var socket: ZMQ.Socket

    @BeforeEach
    fun setUp() {
        mockkConstructor(ZContext::class)

        socket = mockk(relaxed = true)

        every {
            ZContext().createSocket(SocketType.REP)
        } returns socket

        // reset the scope
        WorkerManager.managerScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `Manager should not reply when client request is ill`() {
        every {
            socket.recvStr()
        } returns """{"id": "foo", "type": "bar"}"""

        WorkerManager.pollRequest(socket, "host", 2222, 3333)

        verify(exactly = 0) {
            socket.send(any<String>())
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `Manager should add worker to connected on successful send`() {
        val workerId = "plugin1"

        every {
            socket.recvStr()
        } returns """{"id": "$workerId", "type": "push"}"""

        every {
            socket.send(any<String>())
        } returns true

        WorkerManager.pollRequest(socket, "host", 2222, 3333)

        assertTrue(workerId in WorkerManager.workerIds)
    }

    @Test
    fun `Manager should not add worker to connected on failed send`() {
        val workerId = "plugin1"

        every {
            socket.recvStr()
        } returns """{"id": "$workerId", "type": "push"}"""

        every {
            socket.send(any<String>())
        } returns false

        WorkerManager.pollRequest(socket, "host", 2222, 3333)

        assertTrue(workerId !in WorkerManager.workerIds)
    }

    @Test
    fun `Manager should send port of sink when requesting push`() {
        every {
            socket.recvStr()
        } returns """{"id": "plugin", "type": "push"}"""

        val slot = slot<String>()

        every {
            socket.send(capture(slot))
        } returns true

        val hostname = "localhost"
        val sinkPort = 5555

        WorkerManager.pollRequest(socket, hostname, sinkPort, 3333)

        val json = readJSONContent<Map<String, String>>(slot.captured)

        assertEquals("tcp://$hostname:$sinkPort", json["host"])
    }

    @Test
    fun `Manager should send port of ventilator when requesting pull`() {
        every {
            socket.recvStr()
        } returns """{"id": "plugin", "type": "pull"}"""

        val slot = slot<String>()

        every {
            socket.send(capture(slot))
        } returns true

        val hostname = "localhost"
        val ventPort = 4444

        WorkerManager.pollRequest(socket, hostname, 2222, ventPort)

        val json = readJSONContent<Map<String, String>>(slot.captured)

        assertEquals("tcp://$hostname:$ventPort", json["host"])
    }
}
