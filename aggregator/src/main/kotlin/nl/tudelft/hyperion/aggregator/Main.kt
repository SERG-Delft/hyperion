@file:JvmName("Main")

package nl.tudelft.hyperion.aggregator

import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.aggregator.database.Database
import nl.tudelft.hyperion.aggregator.workers.AggregationManager
import nl.tudelft.hyperion.aggregator.workers.startAPIWorker
import nl.tudelft.hyperion.aggregator.workers.startElasticSearchIntakeWorker
import nl.tudelft.hyperion.aggregator.workers.startExpiryWorker
import java.nio.file.Path

private val logger = mu.KotlinLogging.logger { }

/**
 * Main entry point for the aggregator. Loads the configuration,
 * initiates a database connection and sets up runners on different
 * threads for managing intake and database cleanup.
 */
@Suppress("TooGenericExceptionCaught")
fun main() {
    logger.info { "Starting Hyperion Aggregator..." }

    // Load config
    val config = try {
        val config = Configuration.load(Path.of("./aggregator.yaml").toAbsolutePath())
        config.validate() // ensure the config is somewhat valid
    } catch (ex: Exception) {
        logger.error(ex) { "Failed to parse configuration. Does the file exist and is it valid YAML?" }
        return
    }

    try {
        Database.connect(config)
    } catch (ex: Exception) {
        logger.error(ex) {
            "Failed to connect to the database. Ensure that the database" +
                " is running and that the connection URL is correct."
        }
        return
    }

    val aggregationManager = AggregationManager(config)

    logger.info {
        "Hyperion Aggregator running on port ${config.port} with a" +
            " granularity of ${config.granularity} seconds. ^C to exit."
    }

    // Run tasks blocking. Should never return.
    runBlocking {
        joinAll(
            startExpiryWorker(config),
            startAPIWorker(config),
            aggregationManager.startCommitWorker(),
            startElasticSearchIntakeWorker(aggregationManager)
        )
    }
}
