package nl.tudelft.hyperion.aggregator.database

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import nl.tudelft.hyperion.aggregator.Configuration
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.RuntimeException
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
        mockkStatic("org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManagerKt")
        every { transaction<Any>(null, captureLambda()) } answers { }

        Database.connect(
            Configuration("postgresql", 1, 1, 1)
        )

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
        mockkStatic("org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManagerKt")
        every { transaction<Any>(null, captureLambda()) } answers { }

        assertThrows<RuntimeException> {
            Database.connect(
                Configuration("postgresql", 1, 1, 1)
            )
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

        // Mock transaction to just call the original.
        mockkStatic("org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManagerKt")
        every {
            transaction<Any>(null, captureLambda())
        } answers {
            lambda<Transaction.() -> Any>().captured.invoke(mockk())
        }

        // Mock SchemaUtils
        mockkObject(SchemaUtils)
        every { SchemaUtils.create(*anyVararg<Table>()) } returns mockk()

        Database.connect(
            Configuration("postgresql", 1, 1, 1)
        )

        verify(exactly = 1) {
            SchemaUtils.create(AggregationEntries)
        }

        unmockkAll()
    }
}
