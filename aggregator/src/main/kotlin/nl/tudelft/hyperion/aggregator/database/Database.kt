package nl.tudelft.hyperion.aggregator.database

import nl.tudelft.hyperion.aggregator.Configuration
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.Database as ExposedDatabase

class Database {
    companion object {
        private val logger = mu.KotlinLogging.logger { }

        /**
         * Connects to the database using the specified database configuration,
         * creating required tables if needed. Throws an error if connecting fails.
         */
        fun connect(config: Configuration) {
            // Ensure that we have a config URL somewhat decent.
            if (!config.databaseUrl.startsWith("postgresql:")) {
                throw IllegalArgumentException("Configuration database URL must start with `postgresql:`.")
            }

            // Attempt to connect to the database
            try {
                ExposedDatabase.connect("jdbc:${config.databaseUrl}")
            } catch (ex: Exception) {
                throw RuntimeException("Failed to connect to the database. Ensure that the database is running and that the connection URL is correct.", ex)
            }

            // Create tables.
            transaction {
                logger.debug { "Creating database tables..." }

                SchemaUtils.create(AggregationEntries)

                logger.debug { "Database setup complete." }
            }
        }
    }
}