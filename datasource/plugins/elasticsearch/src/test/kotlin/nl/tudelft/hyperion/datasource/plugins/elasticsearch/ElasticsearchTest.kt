package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import io.lettuce.core.RedisClient
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.mockk.*
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.search.SearchHit
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.testcontainers.containers.GenericContainer


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ElasticsearchTest {
//
//    class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
//
//    lateinit var testConfig: Configuration
//    lateinit var mockClient: RestHighLevelClient
//    lateinit var mockRedis: RedisClient
//
//    @BeforeAll
//    fun init() {
//        testConfig = Configuration(
//                5,
//                RedisConfig("localhost", 6379),
//                ElasticsearchConfig(
//                        "host",
//                        "index",
//                        9200,
//                        "http",
//                        false,
//                        "time",
//                        10,
//                        null,
//                        null
//                ),
//                "-config",
//                "elastic"
//        )
//    }
//
//    @BeforeEach
//    fun setUp() {
//        mockClient = mockk(relaxed = true)
//        mockRedis = mockk(relaxed = true)
//
//        // to ensure that the plugin has an output channel
//        every { mockRedis.connect().sync().hget(any(), any()) } returns "output"
//    }
//
//    @Test
//    fun `test search request parameters`() {
//        val hitCount = 10
//        val index = "foo"
//        val req = Elasticsearch.createSearchRequest(index, "time", 1, 1, hitCount)
//
//        assertEquals(hitCount, req.source().size())
//        assertTrue(index in req.indices())
//    }
//
//    @Test
//    fun `test starting a closed instance`() {
//        val es = spyk(Elasticsearch(testConfig, mockClient, mockRedis))
//
//        es.start()
//        es.stop()
//        es.cleanup()
//
//        assertThrows<IllegalStateException> { es.start() }
//    }
//
//    @Test
//    fun `test that stop does not trigger exception on uninitialized instance`() {
//        val es = spyk(Elasticsearch(testConfig, mockClient, mockRedis))
//        assertDoesNotThrow { es.stop() }
//    }
//
//    @Test
//    fun `test sendAsyncCalled after starting`() {
//        val es = spyk(Elasticsearch(testConfig, mockClient, mockRedis))
//
//        es.start()
//
//        // possibly flaky test
//        Thread.sleep(1500)
//
//        verify { mockClient.searchAsync(any(), any(), any()) }
//    }
//
//    @Test
//    fun `test Elasticsearch builder`() {
//        val config = Configuration(
//                5,
//                RedisConfig("localhost", 6379),
//                ElasticsearchConfig(
//                        "host",
//                        "index",
//                        9200,
//                        "http",
//                        false,
//                        "time",
//                        10,
//                        null,
//                        null
//                ),
//                "-config",
//                "elastic"
//        )
//
//        val es = Elasticsearch.build(config)
//
//        assertEquals("host", es.esClient.lowLevelClient.nodes.first().host.hostName)
//        assertEquals(9200, es.esClient.lowLevelClient.nodes.first().host.port)
//        assertEquals("http", es.esClient.lowLevelClient.nodes.first().host.schemeName)
//    }
//
//    @Test
//    fun `test missing authentication arguments in builder`() {
//        val config = Configuration(
//                5,
//                RedisConfig("localhost", 6379),
//                ElasticsearchConfig(
//                        "host",
//                        "index",
//                        9200,
//                        "http",
//                        true,
//                        "time",
//                        10,
//                        null,
//                        "password"
//                ),
//                "-config",
//                "elastic"
//        )
//
//        assertThrows<NullPointerException> { Elasticsearch.build(config) }
//    }
//
//    @Test
//    fun `test correct authentication in builder`() {
//        val config = Configuration(
//                5,
//                RedisConfig("localhost", 6379),
//                ElasticsearchConfig(
//                        "host",
//                        "index",
//                        9200,
//                        "http",
//                        true,
//                        "time",
//                        10_000,
//                        "user",
//                        "password"
//                ),
//                "-config",
//                "elastic"
//        )
//        assertDoesNotThrow { Elasticsearch.build(config) }
//    }
//
//    @Test
//    fun `test sendHit calls publisher`() {
//        val searchHit = SearchHit(1)
//
//        val es = spyk(Elasticsearch(testConfig, mockClient, mockRedis))
//
//        val publisher = mockk<RedisAsyncCommands<String, String>>(relaxed = true)
//        es.publisher = publisher
//
//        es.sendHit(searchHit)
//
//        verify(exactly = 1) { publisher.publish(any(), any()) }
//    }
//
//    @Test
//    fun `test cleanup removes channels if initialized`() {
//        val es = spyk(Elasticsearch(testConfig, mockClient, mockRedis))
//        val publisherConn = mockk<StatefulRedisPubSubConnection<String, String>>(relaxed = true)
//        es.publisherConn = publisherConn
//
//        es.cleanup()
//
//        verify {
//            publisherConn.flushCommands()
//            publisherConn.close()
//            mockRedis.shutdown()
//        }
//    }
//
//    @Test
//    fun `test pubChannel not set during start`() {
//        every { mockRedis.connect().sync().hget(any(), any()) } returns null
//        val es = spyk(Elasticsearch(testConfig, mockClient, mockRedis))
//
//        assertThrows<java.lang.IllegalStateException> { es.start() }
//    }
}