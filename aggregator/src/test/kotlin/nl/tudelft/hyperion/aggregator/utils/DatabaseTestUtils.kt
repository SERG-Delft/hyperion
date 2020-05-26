package nl.tudelft.hyperion.aggregator.utils

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Helper function that runs the specified body with transactions disabled.
 * The bodies of transactions will not be called while in this function.
 */
fun withDisabledTransactions(fn: () -> Unit) {
    mockkStatic("org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManagerKt")

    every {
        transaction<Any>(null, any())
    } answers {}

    fn()

    unmockkStatic("org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManagerKt")
}

/**
 * Helper function that runs the specified body with transactions enabled,
 * but every transaction will be ran using the specified transaction as receiver.
 */
fun withSpecificTransaction(transaction: Transaction, fn: () -> Unit) {
    mockkStatic("org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManagerKt")

    every {
        transaction<Any>(null, captureLambda())
    } answers {
        lambda<Transaction.() -> Any>().captured.invoke(transaction)
    }

    fn()

    unmockkStatic("org.jetbrains.exposed.sql.transactions.ThreadLocalTransactionManagerKt")
}
