package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import nl.tudelft.hyperion.datasource.common.RedisConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ElasticsearchTest {

    lateinit var testConfig: Configuration

    @BeforeEach
    fun init() {
        testConfig = Configuration(
                5,
                RedisConfig("foo", 6379, "bar"),
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

    @Test
    fun `test search request parameters`() {
        val hitCount = 10
        val index = "foo"
        val req = Elasticsearch.createSearchRequest(index, "time", 1, 1, hitCount)

        assertEquals(hitCount, req.source().size())
        assertTrue(index in req.indices())
    }
}