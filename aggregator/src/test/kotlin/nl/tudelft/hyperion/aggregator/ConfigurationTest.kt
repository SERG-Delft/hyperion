package nl.tudelft.hyperion.aggregator

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import nl.tudelft.hyperion.aggregator.utils.TestWithoutLogging
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Files

/**
 * Unit tests the Configuration class.
 * @see Configuration
 */
class ConfigurationTest : TestWithoutLogging() {
    @Test
    fun `Configuration should be able to parse from a file`() {
        val temporaryFile = File.createTempFile("hyperion-aggregator-config", "yaml")
        Files.writeString(
            temporaryFile.toPath(), """
                databaseUrl: "postgresql://localhost/postgres?user=postgres&password=mysecretpassword"
                port: 8081
                granularity: 10 # 10 seconds
                aggregationTtl: 604800 # 7 days
            """.trimIndent()
        )

        val config = Configuration.load(temporaryFile.toPath())
        val expected = Configuration(
            "postgresql://localhost/postgres?user=postgres&password=mysecretpassword",
            8081,
            10,
            604800
        )
        assertEquals(config, expected)
    }

    @Test
    fun `Configuration should be able to load from a string`() {
        val content = """
            databaseUrl: "postgresql://localhost/postgres?user=postgres&password=mysecretpassword"
            port: 8081
            granularity: 10 # 10 seconds
            aggregationTtl: 604800 # 7 days
        """.trimIndent()

        val config = Configuration.parse(content)
        val expected = Configuration(
            "postgresql://localhost/postgres?user=postgres&password=mysecretpassword",
            8081,
            10,
            604800
        )
        assertEquals(config, expected)
    }

    @Test
    fun `Configuration should be able to support comments and empty lines`() {
        val content = """
            # This is a comment
            # More comment
            # Some empty lines follow:
            
            
            
            databaseUrl: "postgresql://localhost/postgres?user=postgres&password=mysecretpassword"
            # port: 8082
            port: 8081
            
            granularity: 10 # 10 seconds
            # 7 days
            aggregationTtl: 604800
        """.trimIndent()

        val config = Configuration.parse(content)
        val expected = Configuration(
            "postgresql://localhost/postgres?user=postgres&password=mysecretpassword",
            8081,
            10,
            604800
        )
        assertEquals(config, expected)
    }

    @Test
    fun `Configuration should not support unknown settings`() {
        val content = """
            databaseUrl: "postgresql://localhost/postgres?user=postgres&password=mysecretpassword"
            port: 8081
            granularity: 10 # 10 seconds
            aggregationTtl: 604800 # 7 days
            unknown-property: true
        """.trimIndent()

        assertThrows<UnrecognizedPropertyException> {
            Configuration.parse(content)
        }
    }

    @TestFactory
    fun `Configuration should throw on invalid settings`() = listOf(
        // Database URL invalid.
        Configuration("a", 1000, 1, 1),

        // Port invalid.
        Configuration("postgresql:", -1000, 1, 2),
        Configuration("postgresql:", 0, 1, 2),
        Configuration("postgresql:", 1, 1, 2),
        Configuration("postgresql:", 100000, 1, 2),

        // Granularity invalid
        Configuration("postgresql:", 1000, -1000, 2),
        Configuration("postgresql:", 1000, 0, 2),

        // Aggregation ttl invalid
        Configuration("postgresql:", 1000, 1, -1000),
        Configuration("postgresql:", 1000, 1, 0),

        // Aggregation TTL less than granularity
        Configuration("postgresql:", 1000, 100, 40)
    ).map {
        DynamicTest.dynamicTest("configuration $it should be invalid") {
            assertThrows<IllegalArgumentException> {
                it.validate()
            }
        }
    }

    @Test
    fun `Configuration validation should succeed for valid configs`() {
        val valid = Configuration(
            "postgresql://localhost/postgres?user=postgres&password=mysecretpassword",
            8081,
            10,
            604800
        )

        // Should return self
        assertEquals(valid.validate(), valid)
    }
}
