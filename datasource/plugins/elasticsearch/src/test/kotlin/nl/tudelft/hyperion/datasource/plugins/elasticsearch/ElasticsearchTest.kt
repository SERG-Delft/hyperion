package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import io.mockk.mockk
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import nl.tudelft.hyperion.datasource.common.DataPluginInitializationException
import org.elasticsearch.client.RestHighLevelClient
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun init() {
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
        es.hasConnectionInformation = false

        suspendingCoroutineThrows(DataPluginInitializationException::class, 100) {
            es.run(it)
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
}