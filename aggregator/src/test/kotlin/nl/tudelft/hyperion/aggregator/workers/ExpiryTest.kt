package nl.tudelft.hyperion.aggregator.workers

import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.aggregator.Configuration
import nl.tudelft.hyperion.aggregator.utils.TestWithoutLogging
import nl.tudelft.hyperion.aggregator.utils.withSpecificTransaction
import org.jetbrains.exposed.sql.Transaction
import org.junit.jupiter.api.Test

class ExpiryTest : TestWithoutLogging() {
    @Test
    fun `Expiry handler should run with the granularity interval`() {
        // Mock Transaction.exec
        val transaction = mockk<Transaction>()

        every {
            transaction.exec(any())
        } answers {
            // Do nothing
        }

        withSpecificTransaction(transaction) {
            // Run expiry handler for 1.1 seconds. It should invoke the deletion twice (once, delay, twice)
            runBlocking {
                val worker = startExpiryWorker(Configuration("a", 1, 1, 1))
                delay(1100L)
                worker.cancelAndJoin()
            }
        }

        // Assert twice.
        verify(exactly = 2) {
            transaction.exec(match { it.contains("DELETE FROM") })
        }

        unmockkAll()
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

        withSpecificTransaction(transaction) {
            // Run expiry handler for 1.1 seconds. It should invoke the deletion twice (once, delay, twice)
            runBlocking {
                val worker = startExpiryWorker(Configuration("a", 1, 1, 1))
                delay(1100L)
                worker.cancelAndJoin()
            }
        }

        // Assert twice.
        verify(exactly = 2) {
            transaction.exec(match { it.contains("DELETE FROM") })
        }

        unmockkAll()
    }
}
