package nl.tudelft.hyperion.pipeline.versiontracker

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ConfigurationTest {

    companion object {
        val mapper = ObjectMapper(YAMLFactory())
    }

    init {
        mapper.registerModule(KotlinModule())
    }

    @Test
    fun `Authentication HTTPS should not allow matching`() {
        val auth1 = Authentication.HTTPS("john", "secret")
        val auth2 = Authentication.HTTPS("john", "secret")

        assertFalse(auth1 == auth2)
    }

    @Test
    fun `Authentication deserializer should correctly deserialize SSH`() {
        val config = """
            type: ssh
            key-path: ./id_rsa
       """.trimIndent()

        val result: Authentication = mapper.readValue(config)

        assertTrue(result is Authentication.SSH)
    }

    @Test
    fun `Authentication deserializer should correctly deserialize HTTPS`() {
        val config = """
            type: https
            username: john
            password: john123
       """.trimIndent()

        val result: Authentication = mapper.readValue(config)

        assertTrue(result is Authentication.HTTPS)
    }

    @ParameterizedTest
    @CsvSource(
        "type: https\nusername: john",
        "type: https\npassword: secret",
        "type: https"
    )
    fun `Authentication deserializer should throw exception when HTTPS field is missing`(config: String) {
        assertThrows<InvalidFormatException> { mapper.readValue<Authentication>(config) }
    }

    @Test
    fun `Authentication deserializer should throw exception when SSH keyPath is missing`() {
        val config = """
            type: ssh
       """.trimIndent()
        assertThrows<InvalidFormatException> { mapper.readValue<Authentication>(config) }
    }

    @Test
    fun `Authentication deserializer should throw exception when type is unknown`() {
        val config = """
            type: invalid
       """.trimIndent()
        assertThrows<InvalidFormatException> { mapper.readValue<Authentication>(config) }
    }

    @Test
    fun `Authentication deserializer should throw exception when missing type`() {
        val config = """
            invalidField: 123
       """.trimIndent()
        assertThrows<MismatchedInputException> { mapper.readValue<Authentication>(config) }
    }
}