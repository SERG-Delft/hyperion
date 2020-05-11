package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import nl.tudelft.hyperion.pluginmanager.RedisConfig
import org.elasticsearch.client.RestHighLevelClient
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.testcontainers.containers.GenericContainer


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ElasticsearchTest {

    class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

    lateinit var testConfig: Configuration
    lateinit var redis: KGenericContainer

    @BeforeAll
    fun init() {
        redis = KGenericContainer("redis:6.0-alpine").withExposedPorts(6379)
        redis.start()

        testConfig = Configuration(
                5,
                RedisConfig(redis.containerIpAddress, redis.firstMappedPort),
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
                null,
                "elastic"
        )

        val redisClient = RedisClient.create(RedisURI.create("localhost", testConfig.redis.port!!))
        // The redis client is not injectable so this workaround is used
        val sync = redisClient.connect().sync()
        sync.hset("elastic-config", "publisher", "true")
        sync.hset("elastic-config", "subscriber", "false")
        sync.hset("elastic-config", "pubChannel", "output")
        redisClient.shutdown()
    }

    @AfterAll
    fun cleanup() {
        redis.stop()
    }

    @Test
    fun `test search request parameters`() {
        val hitCount = 10
        val index = "foo"
        val req = Elasticsearch.createSearchRequest(index, "time", 1, 1, hitCount)

        assertEquals(hitCount, req.source().size())
        assertTrue(index in req.indices())
    }

    @Test
    fun `test starting a closed instance`() {
        val mockClient = mockk<RestHighLevelClient>(relaxed = true)
        val es = Elasticsearch(testConfig, mockClient)

        es.start()
        es.stop()
        es.cleanup()

        assertThrows<IllegalStateException> { es.start() }
    }

    @Test
    fun `test that stop does not trigger exception on uninitialized instance`() {
        val mockClient = mockk<RestHighLevelClient>(relaxed = true)
        val es = Elasticsearch(testConfig, mockClient)
        assertDoesNotThrow { es.stop() }
    }

    @Test
    fun `test sendAsyncCalled after starting`() {
        val mockClient = mockk<RestHighLevelClient>(relaxed = true)
        val es = Elasticsearch(testConfig, mockClient)

        es.start()

        // possibly flaky test
        Thread.sleep(1500)

        verify { mockClient.searchAsync(any(), any(), any()) }
    }

    @Test
    fun `test Elasticsearch builder`() {
        val config = Configuration(
                5,
                RedisConfig(redis.containerIpAddress, redis.firstMappedPort),
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
                null,
                "elastic"
        )

        val es = Elasticsearch.build(config)

        assertEquals("host", es.client.lowLevelClient.nodes.first().host.hostName)
        assertEquals(9200, es.client.lowLevelClient.nodes.first().host.port)
        assertEquals("http", es.client.lowLevelClient.nodes.first().host.schemeName)
    }

    @Test
    fun `test missing authentication arguments in builder`() {
        val config = Configuration(
                5,
                RedisConfig(redis.containerIpAddress, redis.firstMappedPort),
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
                null,
                "elastic"
        )

        assertThrows<NullPointerException> { Elasticsearch.build(config) }
    }

    @Test
    fun `test correct authentication in builder`() {
        val config = Configuration(
                5,
                RedisConfig(redis.containerIpAddress, redis.firstMappedPort),
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
                null,
                "elastic"
        )
        assertDoesNotThrow { Elasticsearch.build(config) }
    }

    /**
     * Test work function to comply with Jacoco
     */
    @Test
    fun `test work function not used`() {
        val mockClient = mockk<RestHighLevelClient>(relaxed = true)
        val es = spyk(Elasticsearch(testConfig, mockClient))
        assertThrows<java.lang.IllegalStateException> { es.work("test") }
    }
}