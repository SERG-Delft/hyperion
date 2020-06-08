package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withTimeout
import nl.tudelft.hyperion.datasource.common.DataPluginInitializationException
import nl.tudelft.hyperion.pipeline.PeerConnectionInformation
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import org.elasticsearch.client.RestHighLevelClient
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.zeromq.ZContext
import java.util.Timer
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
            PipelinePluginConfiguration("Elasticsearch", "localhost:5555"),
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
            )
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
            PipelinePluginConfiguration("Elasticsearch", "localhost:5555"),
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
            )
        )

        assertThrows<NullPointerException> { Elasticsearch.build(config) }
    }

    @Test
    fun `Builder should not throw exception if authentication is correct`() {
        val config = Configuration(
            5,
            PipelinePluginConfiguration("Elasticsearch", "localhost:5555"),
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
            )
        )

        assertDoesNotThrow { Elasticsearch.build(config) }
    }

    @Test
    fun `Run should cleanup if sender is cancelled`() {
        mockkConstructor(ZContext::class)

        val senderJob = mockk<Job>(relaxed = true)

        val peerAddress = "tcp://localhost:12345"

        val es = spyk(Elasticsearch(testConfig, mockClient))
        es.hasConnectionInformation = true
        es.subConnectionInformation = PeerConnectionInformation(null, false)
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
