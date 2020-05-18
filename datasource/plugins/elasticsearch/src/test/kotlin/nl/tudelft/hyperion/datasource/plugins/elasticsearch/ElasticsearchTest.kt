package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runBlockingTest
import nl.tudelft.hyperion.datasource.common.DataPluginInitializationException
import org.elasticsearch.client.RestHighLevelClient
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ElasticsearchTest {

    lateinit var testConfig: Configuration
    lateinit var mockClient: RestHighLevelClient

    companion object {
        fun <T : Any> suspendingCoroutineThrows(
                expected: KClass<T>,
                timeout: Long,
                closure: (CoroutineContext) -> Job
        ) {
            var success = false

            val assertHandler = CoroutineExceptionHandler { _, exception ->
                assertEquals(exception::class, expected)
                success = true
            }

            runBlocking {
                withTimeout(timeout) {
                    closure(assertHandler).join()
                }
            }

            assertTrue(success)
        }
    }

    @BeforeAll
    fun globalSetUp() {
        testConfig = Configuration(
                5,
                ManagerConfig("localhost", 5555),
                ElasticsearchConfig(
                        "host",
                        "index",
                        9200,
                        "http",
                        false,
                        "time",
                        10,
                        null,
                        null
                ),
                "Elasticsearch"
        )
    }

    @BeforeEach
    fun setUp() {
        mockClient = mockk(relaxed = true)
    }

    @Test
    fun `Correct search parameters after createSearchRequest`() {
        val hitCount = 10
        val index = "foo"
        val req = Elasticsearch.createSearchRequest(index, "time", 1, 1, hitCount)

        assertEquals(hitCount, req.source().size())
        assertTrue(index in req.indices())
    }

    @Test
    fun `Starting a closed instance should throw exception`() {
        val es = Elasticsearch(testConfig, mockClient)
        es.cleanup()

        suspendingCoroutineThrows(IllegalStateException::class, 100) {
            es.run(it)
        }
    }

    @Test
    fun `Calling stop on new instance should not throw exception`() {
        val es = Elasticsearch(testConfig, mockClient)
        assertDoesNotThrow { es.stop() }
    }

    @Test
    fun `Calling cleanup on new instance should not throw exception`() {
        val es = Elasticsearch(testConfig, mockClient)
        assertDoesNotThrow { es.cleanup() }
    }

    @Test
    fun `Starting an instance without querying info should throw exception`() {
        val es = Elasticsearch(testConfig, mockClient)

        suspendingCoroutineThrows(DataPluginInitializationException::class, 100) {
            es.run(it)
        }
    }

    @Test
    fun `Calling stop on a started timer should cancel it`() {
        val es = Elasticsearch(testConfig, mockClient)
        val mockTimer = mockk<Timer>(relaxed = true)
        es.timer = mockTimer

        es.stop()

        verify {
            mockTimer.cancel()
        }
    }

    @Test
    fun `Builder should correctly add fields from config`() {
        val es = Elasticsearch.build(testConfig)

        assertEquals("host", es.esClient.lowLevelClient.nodes.first().host.hostName)
        assertEquals(9200, es.esClient.lowLevelClient.nodes.first().host.port)
        assertEquals("http", es.esClient.lowLevelClient.nodes.first().host.schemeName)
    }

    @Test
    fun `Builder should throw exception if partial authentication is given`() {
        val config = Configuration(
                5,
                ManagerConfig("localhost", 5555),
                ElasticsearchConfig(
                        "host",
                        "index",
                        9200,
                        "http",
                        true,
                        "time",
                        10,
                        null,
                        "password"
                ),
                "Elasticsearch"
        )

        assertThrows<NullPointerException> { Elasticsearch.build(config) }
    }

    @Test
    fun `Builder should not throw exception if authentication is correct`() {
        val config = Configuration(
                5,
                ManagerConfig("localhost", 5555),
                ElasticsearchConfig(
                        "host",
                        "index",
                        9200,
                        "http",
                        true,
                        "time",
                        10_000,
                        "user",
                        "password"
                ),
                "Elasticsearch"
        )

        assertDoesNotThrow { Elasticsearch.build(config) }
    }

    @Test
    fun `queryConnectionInformation should succeed if sent correct peer information`() {
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

        val es = Elasticsearch(testConfig, mockClient)

        es.queryConnectionInformation()

        verify {
            socket.connect("tcp://${testConfig.zmq.address}")
            socket.send("""{"id":"Elasticsearch","type":"out"}""")
        }

        assertNotNull(es.pubConnectionInformation)

        unmockkAll()
    }

    @Test
    fun `queryConnectionInformation should throw exception if sent incorrect peer information`() {
        mockkConstructor(ZContext::class)

        val socket = mockk<ZMQ.Socket>(relaxed = true)

        every {
            ZContext().createSocket(SocketType.REQ)
        } returns socket

        every {
            socket.recvStr()
        } returns """
            {"host":"tcp://*:12345","isBind":"foo"}
        """.trimIndent()

        val es = Elasticsearch(testConfig, mockClient)

        assertThrows<Exception> { es.queryConnectionInformation() }
        assertEquals(null, es.pubConnectionInformation)

        unmockkAll()
    }

    @Test
    fun `Sender socket should bind if isBind is set to true`() {
        mockkConstructor(ZContext::class)

        val socket = mockk<ZMQ.Socket>(relaxed = true)
        val channel = mockk<Channel<String>>(relaxed = true)

        coEvery {
            channel.receive()
        } coAnswers {
            """{"foo": "bar"}"""
        }

        every {
            ZContext().createSocket(SocketType.PUSH)
        } returns socket

        val peerAddress = "tcp://localhost:12345"

        val es = Elasticsearch(testConfig, mockClient)
        es.pubConnectionInformation = PeerConnectionInformation(peerAddress, true)

        runBlocking {
            // possibly flaky
            withTimeout(1000) {
                es.runSender(channel = channel)
            }
        }

        coVerify {
            socket.bind(peerAddress)
        }

        unmockkAll()
    }

    @Test
    @ExperimentalCoroutinesApi
    fun `Run should cleanup if sender is cancelled`() {
        mockkConstructor(ZContext::class)

        val senderJob = mockk<Job>(relaxed = true)

        val peerAddress = "tcp://localhost:12345"

        val es = spyk(Elasticsearch(testConfig, mockClient))
        es.pubConnectionInformation = PeerConnectionInformation(peerAddress, false)

        val job = es.run(Dispatchers.Unconfined)

        every {
            es.runSender(any())
        } returns senderJob

        runBlockingTest {
            job.cancelAndJoin()
        }

        coVerify {
            es.stop()
            es.cleanup()
        }

        unmockkAll()
    }
}
