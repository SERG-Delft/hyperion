package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.lang.IllegalArgumentException
import kotlin.reflect.full.memberProperties
import kotlin.reflect.KClass

/**
 * Tests the parsing of configuration files.
 */
class ConfigurationTest {

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

    @Test
    fun `test valid config`() {
        val config =
                """
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
                  channel: foo
                """.trimIndent()

        assertDoesNotThrow { Configuration.parse(config) }
    }

    @ParameterizedTest
    @CsvSource(
            "port, 9200",
            "scheme, http"
    )
    fun `test optional elasticsearch fields`(param: String, value: Any) {
        val config = Configuration.parse(
                """
                poll_interval: 5
                elasticsearch:
                  hostname: foo
                  index: logs
                  timestamp_field: "@timestamp"
                  authentication: no
                  response_hit_count: 10
                redis:
                  host: localhost
                  channel: foo
                """.trimIndent())

        val result = ElasticsearchConfig::class.getProp(param, config.es)
        assertEquals(value, result.toString())
    }

    @Test
    fun `test invalid field type`() {
        val config =
                """
                poll_interval: foo
                elasticsearch:
                  hostname: foo
                  index: logs
                  timestamp_field: "@timestamp"
                  authentication: no
                  response_hit_count: 10
                redis:
                  host: localhost
                  channel: foo
                """.trimIndent()

        assertThrows<InvalidFormatException> { Configuration.parse(config) }
    }

    @Test
    fun `test missing elasticsearch field`() {
        val config =
                """
                poll_interval: 42
                redis:
                  host: localhost
                  channel: foo
                """.trimIndent()

        assertThrows<JsonMappingException> { Configuration.parse(config) }
    }

    @Test
    fun `test missing username for authentication`() {
        val config =
                """
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
                  channel: foo
                """.trimIndent()

        assertThrows<IllegalArgumentException> { Configuration.parse(config) }
    }
}