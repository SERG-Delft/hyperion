package nl.tudelft.hyperion.pipeline.versiontracker

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ConfigurationTest {

    companion object {
        val mapper = ObjectMapper(YAMLFactory())
    }

    init {
        mapper.registerModule(KotlinModule())
    }

    @Test
    fun `Authentication deserializer should correctly deserialize SSH`() {
        val config = """
            type: ssh
            keyPath: ./id_rsa
       """.trimIndent()

        val expected = Authentication.SSH("./id_rsa")

        val result: Authentication = mapper.readValue(config)

        assertEquals(expected, result)
    }

    @Test
    fun `Authentication deserializer should correctly deserialize HTTPS`() {
        val config = """
            type: https
            username: john
            password: john123
       """.trimIndent()

        val expected = Authentication.HTTPS("john", "john123")

        val result: Authentication = mapper.readValue(config)

        assertEquals(expected, result)
    }

    @Test
    fun `Authentication deserializer should throw exception when HTTPS field is missing`() {
        val config = """
            type: https
            username: john
       """.trimIndent()
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