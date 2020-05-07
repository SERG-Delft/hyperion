package nl.tudelft.hyperion.aggregator.database

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import nl.tudelft.hyperion.aggregator.Configuration
import nl.tudelft.hyperion.aggregator.utils.withDisabledTransactions
import nl.tudelft.hyperion.aggregator.utils.withSpecificTransaction
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import org.jetbrains.exposed.sql.Database as ExposedDatabase

class DatabaseTest {
    @Test
    fun `Database create connects with the configuration path`() {
        // Mock Database.connect
        mockkObject(ExposedDatabase.Companion)
        every {
            ExposedDatabase.connect(any(), any(), any(), any(), any(), any())
        } returns mockk()

        // Mock transaction to do nothing.
        withDisabledTransactions {
            Database.connect(
                Configuration("postgresql", 1, 1, 1)
            )
        }

        verify(exactly = 1) {
            ExposedDatabase.connect("jdbc:postgresql", any(), any(), any(), any(), any())
        }

        unmockkAll()
    }

    @Test
    fun `Database rethrows connection errors`() {
        // Mock Database.connect
        mockkObject(ExposedDatabase.Companion)
        every {
            ExposedDatabase.connect(any(), any(), any(), any(), any(), any())
        } throws SQLException("Oh no couldn't connect!")

        // Mock transaction to do nothing.
        withDisabledTransactions {
            assertThrows<RuntimeException> {
                Database.connect(
                    Configuration("postgresql", 1, 1, 1)
                )
            }
        }

        unmockkAll()
    }

    @Test
    fun `Database creates schema if it does not already exist`() {
        // Mock Database.connect
        mockkObject(ExposedDatabase.Companion)
        every {
            ExposedDatabase.connect(any(), any(), any(), any(), any(), any())
        } returns mockk()

        // Mock SchemaUtils
        mockkObject(SchemaUtils)
        every { SchemaUtils.create(*anyVararg<Table>()) } returns mockk()

        withSpecificTransaction(mockk()) {
            Database.connect(
                Configuration("postgresql", 1, 1, 1)
            )
        }

        verify(exactly = 1) {
            SchemaUtils.create(AggregationEntries)
        }

        unmockkAll()
    }
}
