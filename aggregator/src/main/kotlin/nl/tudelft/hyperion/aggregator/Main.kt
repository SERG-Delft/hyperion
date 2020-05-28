@file:JvmName("Main")

package nl.tudelft.hyperion.aggregator

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.aggregator.database.Database
import nl.tudelft.hyperion.aggregator.intake.ZMQIntake
import nl.tudelft.hyperion.aggregator.workers.AggregationManager
import nl.tudelft.hyperion.aggregator.workers.startAPIWorker
import nl.tudelft.hyperion.aggregator.workers.startExpiryWorker
import java.nio.file.Path

private val logger = mu.KotlinLogging.logger { }

/**
 * Main entry point for the aggregator. Loads the configuration,
 * initiates a database connection and sets up runners on different
 * threads for managing intake and database cleanup.
 *
 * Note that this is a coroutine that can be cancelled. It is called
 * to be ran blocking by the actual main function
 */
@Suppress("TooGenericExceptionCaught")
fun coMain(configPath: String) = GlobalScope.launch {
    logger.info { "Starting Hyperion Aggregator..." }

    // Load config
    val config = try {
        val config = Configuration.load(
            Path.of(configPath).toAbsolutePath()
        )
        config.validate() // ensure the config is somewhat valid
    } catch (ex: Exception) {
        logger.error(ex) { "Failed to parse configuration. Does the file exist and is it valid YAML?" }
        return@launch
    }

    try {
        Database.connect(config)
    } catch (ex: Exception) {
        logger.error(ex) {
            "Failed to connect to the database. Ensure that the database" +
                " is running and that the connection URL is correct."
        }
        return@launch
    }

    val aggregationManager = AggregationManager(config)

    logger.info {
        "Hyperion Aggregator running on port ${config.port} with a" +
            " granularity of ${config.granularity} seconds. ^C to exit."
    }

    val intake = ZMQIntake(config.pipeline, aggregationManager)
    intake.setup()

    joinAll(
        startExpiryWorker(config),
        startAPIWorker(config),
        intake.listen(),
        aggregationManager.startCommitWorker()
    )
}

/**
 * Actual main function. Simply runs the coroutine version of main blocking.
 */
fun main() {
    runBlocking {
        coMain(System.getenv("HYPERION_AGGREGATOR_CONFIG") ?: "./aggregator.yaml").join()
    }
}
