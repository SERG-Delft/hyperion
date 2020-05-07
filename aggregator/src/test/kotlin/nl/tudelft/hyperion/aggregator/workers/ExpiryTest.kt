package nl.tudelft.hyperion.aggregator.workers

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.aggregator.Configuration
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test

class ExpiryTest {
    @Test
    fun `Expiry handler should run with the granularity interval`() {
        // Mock Transaction.exec
        val transaction = mockk<Transaction>()

        every {
            transaction.exec(any())
        } answers {
            // Do nothing
        }

        // Mock transaction { }
        mockkStatic("org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManagerKt")
        every {
            transaction<Any>(null, captureLambda())
        } answers {
            lambda<Transaction.() -> Any>().captured.invoke(transaction)
        }

        // Run expiry handler for 1.1 seconds. It should invoke the deletion twice (once, delay, twice)
        runBlocking {
            val worker = startExpiryWorker(Configuration("a", 1, 1, 1))
            delay(1100L)
            worker.cancel()
        }

        // Assert twice.
        verify(exactly = 2) {
            transaction.exec(match { it.contains("DELETE FROM") })
        }
    }

    @Test
    fun `Expiry handler should continue even if removal fails`() {
        // Mock Transaction.exec
        val transaction = mockk<Transaction>()
        var calledOnce = false

        every {
            transaction.exec(any())
        } answers {
            // Throw on first call. Else, do nothing.
            if (!calledOnce) {
                calledOnce = true
                throw RuntimeException("Something bad happened!")
            }
        }

        // Mock transaction { }
        mockkStatic("org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManagerKt")
        every {
            transaction<Any>(null, captureLambda())
        } answers {
            lambda<Transaction.() -> Any>().captured.invoke(transaction)
        }

        // Run expiry handler for 1.1 seconds. It should invoke the deletion twice (once, delay, twice)
        runBlocking {
            val worker = startExpiryWorker(Configuration("a", 1, 1, 1))
            delay(1100L)
            worker.cancel()
        }

        // Assert twice.
        verify(exactly = 2) {
            transaction.exec(match { it.contains("DELETE FROM") })
        }
    }
}
