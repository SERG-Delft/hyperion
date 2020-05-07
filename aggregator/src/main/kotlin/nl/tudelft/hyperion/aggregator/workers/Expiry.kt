package nl.tudelft.hyperion.aggregator.workers

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.aggregator.Configuration
import nl.tudelft.hyperion.aggregator.database.AggregationEntries
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Starts a new expiry worker that runs in the background and automatically removes
 * database entries that are older than the values specified in the configuration.
 */
@Suppress("TooGenericExceptionCaught")
fun startExpiryWorker(configuration: Configuration) = GlobalScope.launch {
    val logger = mu.KotlinLogging.logger {}

    logger.debug { "Starting expiry worker..." }

    while (isActive) {
        logger.debug { "Running deletion..." }

        try {
            // Delete rows that are too old.
            transaction {
                // Cannot represent the interval syntax using exposed, unfortunately.
                // This is not vulnerable to sql injection as all the properties are hardcoded.
                exec(
                    "DELETE FROM ${AggregationEntries.tableName} WHERE ${AggregationEntries.timestamp.name} <" +
                        " now() - interval '${configuration.aggregationTtl} seconds'"
                )
            }
        } catch (ex: Exception) {
            // Catch error, but keep loop running.
            logger.error(ex) { "Failed to delete expired rows." }
        }

        logger.debug { "Deleted expired rows." }

        // Wait for our granularity to process. If the granularity stayed constant,
        // we will have a new row to remove from the database.
        delay(configuration.granularity * 1000L)
    }

    // Never returns.
}
