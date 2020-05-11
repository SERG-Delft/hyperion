package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import nl.tudelft.hyperion.pluginmanager.RedisConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Tests the parsing of configuration files.
 */
class ConfigurationTest {

    lateinit var rawConfig: String
    lateinit var testConfig: Configuration

    companion object {
        /**
         * Gets a property from a KClass.
         * Throws a null exception if it does not exist.
         *
         * @param T class type
         * @param name the name of the property to get
         * @param receiver object to get the property from
         * @return the value of the property
         */
        fun <T : Any> KClass<T>.getProp(name: String, receiver: T): Any {
            return memberProperties.first { it.name == name }.get(receiver)!!
        }
    }

    @BeforeEach
    fun init() {
        rawConfig =
                """
                name: elastic
                poll_interval: 5
                elasticsearch:
                  hostname: foo
                  index: logs
                  port: 9200
                  scheme: http
                  timestamp_field: "@timestamp"
                  authentication: no
                  response_hit_count: 10
                redis:
                  host: localhost
                  port: 6379
                """.trimIndent()

        testConfig = Configuration(
                5,
                RedisConfig(
                        "localhost",
                        6379
                ),
                ElasticsearchConfig(
                        "foo",
                        "logs",
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
    }

    @Test
    fun `test valid config`() {
        assertDoesNotThrow { Configuration.parse(rawConfig) }
    }

    @ParameterizedTest
    @CsvSource(
            "port, 9200",
            "scheme, http"
    )
    fun `test optional elasticsearch fields`(param: String, value: Any) {
        val config = Configuration.parse(
                """
                name: elastic
                poll_interval: 5
                elasticsearch:
                  hostname: foo
                  index: logs
                  timestamp_field: "@timestamp"
                  authentication: no
                  response_hit_count: 10
                redis:
                  host: localhost
                """.trimIndent())

        val result = ElasticsearchConfig::class.getProp(param, config.es)
        assertEquals(value, result.toString())
    }

    @Test
    fun `test invalid field type`() {
        val config =
                """
                name: elastic
                poll_interval: foo
                elasticsearch:
                  hostname: foo
                  index: logs
                  timestamp_field: "@timestamp"
                  authentication: no
                  response_hit_count: 10
                redis:
                  host: localhost
                """.trimIndent()

        assertThrows<InvalidFormatException> { Configuration.parse(config) }
    }

    @Test
    fun `test missing elasticsearch field`() {
        val config =
                """
                name: elastic
                poll_interval: 42
                redis:
                  host: localhost
                """.trimIndent()

        assertThrows<JsonMappingException> { Configuration.parse(config) }
    }

    @Test
    fun `test missing username for authentication`() {
        val config =
                """
                name: elastic
                poll_interval: 10
                elasticsearch:
                  hostname: foo
                  index: logs
                  timestamp_field: "@timestamp"
                  authentication: yes
                  response_hit_count: 10
                  password: correcthorsebatterystaple
                redis:
                  host: localhost
                """.trimIndent()

        assertThrows<IllegalArgumentException> { Configuration.parse(config) }
    }

    @Test
    fun `test illegal port Elasticsearch`() {
        testConfig.es.port = -1
        assertThrows<IllegalArgumentException> { testConfig.es.verify() }
    }

    @Test
    fun `test illegal scheme Elasticsearch`() {
        testConfig.es.scheme = "foo"
        assertThrows<IllegalArgumentException> { testConfig.es.verify() }
    }

    @Test
    fun `test illegal responseCount Elasticsearch()`() {
        testConfig.es.responseHitCount = -1
        assertThrows<IllegalArgumentException> { testConfig.es.verify() }
    }

    @Test
    fun `test illegal arguments Configuration`() {
        testConfig.pollInterval = -1
        assertThrows<IllegalArgumentException> { testConfig.verify() }
    }

    @Test
    fun `test configuration loading from file`() {
        var file: File? = null

        try {
            file = createTempFile(suffix = ".yml")
            file.writeText(rawConfig)
            assertDoesNotThrow { Configuration.load(file.toPath()) }
        } finally {
            file?.delete()
        }
    }
}