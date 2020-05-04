@file:JvmName("Main")
package nl.tudelft.hyperion.aggregator

import nl.tudelft.hyperion.aggregator.database.Database
import java.nio.file.Path

private val logger = mu.KotlinLogging.logger { }

/**
 * Main entry point for the aggregator. Loads the configuration,
 * initiates a database connection and sets up runners on different
 * threads for managing intake and database cleanup.
 */
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
        logger.error(ex) { "Failed to connect to the database. Ensure that the database is running and that the connection URL is correct." }
        return
    }

    logger.info { "Hyperion Aggregator running on port ${config.port} with a granularity of ${config.granularity} seconds. ^C to exit." }
}