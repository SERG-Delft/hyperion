package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import com.fasterxml.jackson.databind.JsonMappingException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.converter.ArgumentConversionException
import org.junit.jupiter.params.converter.ConvertWith
import org.junit.jupiter.params.converter.DefaultArgumentConverter
import org.junit.jupiter.params.converter.SimpleArgumentConverter
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties


/**
 * Tests the parsing of configuration files.
 */
class ConfigurationTest {

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

    class NullableConverter : SimpleArgumentConverter() {
        @Throws(ArgumentConversionException::class)
        override fun convert(source: Any, targetType: Class<*>?): Any? {
            return if ("null" == source) {
                null
            } else DefaultArgumentConverter.INSTANCE.convert(source, targetType)
        }
    }

    @BeforeEach
    fun setUp() {

        testConfig = Configuration(
                5,
                ManagerConfig(
                        "localhost",
                        5555
                ),
                ElasticsearchConfig(
                        "foo",
                        "logs",
                        9200,
                        "http",
                        false,
                        "@timestamp",
                        10,
                        null,
                        null
                ),
                "Elasticsearch"
        )
    }

    @Test
    fun `Valid config should parse`() {
        val expected = Configuration(
                5,
                ManagerConfig(
                        "localhost",
                        5555
                ),
                ElasticsearchConfig(
                        "foo",
                        "logs",
                        9200,
                        "http",
                        false,
                        "@timestamp",
                        10,
                        null,
                        null
                ),
                "Elasticsearch"
        )

        val actual = Configuration.parse(
                """
            id: Elasticsearch
            poll_interval: 5
            elasticsearch:
              hostname: foo
              index: logs
              port: 9200
              scheme: http
              timestamp_field: "@timestamp"
              authentication: no
              response_hit_count: 10
            zmq:
              host: localhost
              port: 5555
            """.trimIndent())

        assertEquals(expected, actual)
    }

    @ParameterizedTest
    @CsvSource(
            "port, 9200",
            "scheme, http"
    )
    fun `Optional fields should be set to default if missing`(param: String, expected: Any) {
        val config = Configuration.parse(
                """
                id: Elasticsearch
                poll_interval: 5
                elasticsearch:
                  hostname: foo
                  index: logs
                  timestamp_field: "@timestamp"
                  authentication: no
                  response_hit_count: 10
                zmq:
                    host: localhost
                    port: 5555
                """.trimIndent())

        val result = ElasticsearchConfig::class.getProp(param, config.es)
        assertEquals(expected, result.toString())
    }

    @Test
    fun `Illegal pollInterval should throw exception during parse`() {
        val config =
                """
                id: Elasticsearch
                poll_interval: -1
                elasticsearch:
                  hostname: foo
                  index: logs
                  timestamp_field: "@timestamp"
                  authentication: no
                  response_hit_count: 10
                zmq:
                    host: localhost
                    port: 5555
                """.trimIndent()

        assertThrows<IllegalArgumentException> { Configuration.parse(config) }
    }

    @Test
    fun `Missing elasticsearch field should throw exception during parse()`() {
        val config =
                """
                id: elastic
                poll_interval: 42
                zmq:
                  host: localhost
                  port: 5555
                """.trimIndent()

        assertThrows<JsonMappingException> { Configuration.parse(config) }
    }

    @Test
    fun `Missing username should throw exception during parse()`() {
        val config =
                """
                id: elastic
                poll_interval: 10
                elasticsearch:
                  hostname: foo
                  index: logs
                  timestamp_field: "@timestamp"
                  authentication: yes
                  response_hit_count: 10
                  password: correcthorsebatterystaple
                zmq:
                  host: localhost
                  port: 5555
                """.trimIndent()

        assertThrows<IllegalArgumentException> { Configuration.parse(config) }
    }

    @ParameterizedTest
    @CsvSource(
            "false, foo, bar, true",
            "false, null, null, true",
            "true, foo, bar, true",
            "true, foo, null, false",
            "true, null, bar, false",
            "true, null, null, false"
    )
    fun `Check legal authentication combos`(
            authentication: Boolean,
            @ConvertWith(NullableConverter::class)
            username: String?,
            @ConvertWith(NullableConverter::class)
            password: String?,
            shouldSucceed: Boolean
    ) {
        testConfig.es.authentication = authentication
        testConfig.es.username = username
        testConfig.es.password = password

        if (shouldSucceed) {
            assertDoesNotThrow { testConfig.es.verify() }
        } else {
            assertThrows<IllegalArgumentException> { testConfig.es.verify() }
        }
    }

    @ParameterizedTest
    @CsvSource(
            "-1, false",
            "80, true",
            "80000, false"
    )
    fun `Check legal ports during verify`(port: Int, shouldSucceed: Boolean) {
        testConfig.es.port = port

        if (shouldSucceed) {
            assertDoesNotThrow { testConfig.es.verify() }
        } else {
            assertThrows<IllegalArgumentException> { testConfig.es.verify() }
        }
    }

    @ParameterizedTest
    @CsvSource(
            "foo, false",
            "http, true",
            "https, true"
    )
    fun `Check legal schemes during verify`(scheme: String, shouldSucceed: Boolean) {
        testConfig.es.scheme = scheme

        if (shouldSucceed) {
            assertDoesNotThrow { testConfig.es.verify() }
        } else {
            assertThrows<IllegalArgumentException> { testConfig.es.verify() }
        }
    }

    @Test
    fun `Illegal responseHitCount should throw exception`() {
        testConfig.es.responseHitCount = -1
        assertThrows<IllegalArgumentException> { testConfig.es.verify() }
    }

    @Test
    fun `Illegal poll_interval should throw exception`() {
        testConfig.pollInterval = -1
        assertThrows<IllegalArgumentException> { testConfig.verify() }
    }

    @Test
    fun `Loading configuration from file should succeed`() {
        var file: File? = null

        try {
            file = createTempFile(suffix = ".yml")
            file.writeText("""
                id: Elasticsearch
                poll_interval: 5
                elasticsearch:
                  hostname: foo
                  index: logs
                  timestamp_field: "@timestamp"
                  authentication: no
                  response_hit_count: 10
                zmq:
                    host: localhost
                    port: 5555
                """.trimIndent())

            assertEquals(testConfig, Configuration.load(file.toPath()))
        } finally {
            file?.delete()
        }
    }

    @Test
    fun `address field in ManagerConfig should be correct`() {
        val config = ManagerConfig("foo", 123)
        assertEquals("foo:123", config.address)
    }
}