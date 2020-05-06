package nl.tudelft.hyperion.aggregator.workers

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nl.tudelft.hyperion.aggregator.Configuration
import nl.tudelft.hyperion.aggregator.api.LogEntry
import nl.tudelft.hyperion.aggregator.database.AggregationEntries
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

private val logger = mu.KotlinLogging.logger {}

/**
 * Manages the aggregation tasks for {@link LogEntry} entries. This manager
 * provides a method for consuming log entries, such that an external worker
 * (usually the channel consumer/redis) can provide entries. Is responsible
 * for aggregating intermediate values and committing them to the database.
 */
class AggregationManager(private val configuration: Configuration) {
    /**
     * Manages intermediate aggregates. Is a map of project: version: aggregates.
     */
    private val aggregateMap: MutableMap<String, MutableMap<String, IntermediateAggregates>> = mutableMapOf()

    /**
     * List of all the entries that also sit in {@link aggregateMap}. Used for easy committing.
     */
    private val intermediateAggregates: MutableList<IntermediateAggregates> = mutableListOf()

    /**
     * Mutex used for ensuring concurrent access to the aggregate map.
     */
    private val aggregateLock = Mutex()

    /**
     * Aggregates the specified log entry. Note that this function suspends. You'll
     * need to run it through a primitive like `runBlocking`.
     */
    suspend fun aggregate(entry: LogEntry) {
        aggregateLock.withLock {
            logger.debug { "Aggregating log entry: $entry" }

            val projectAggregates = aggregateMap.getOrPut(entry.project, ::mutableMapOf)
            val versionAggregates = projectAggregates.getOrPut(entry.version, {
                val intermediates = IntermediateAggregates(entry.project, entry.version)
                intermediateAggregates.add(intermediates)

                intermediates
            })

            versionAggregates.aggregate(entry)
        }
    }

    /**
     * Starts a new commit worker that will periodically commit all intermediate
     * aggregates to the database.
     */
    fun startCommitWorker() = GlobalScope.launch {
        logger.debug { "Starting aggregation commit worker..." }

        while (isActive) {
            delay(configuration.granularity * 1000L)
            commit()
        }
    }

    /**
     * Commits the current aggregates to the database and clears the database list.
     * @see IntermediateAggregates.commit
     */
    private suspend fun commit() {
        aggregateLock.withLock {
            logger.debug { "Running aggregation commit..." }

            // Commit every aggregate.
            // TODO: Do this async?
            intermediateAggregates.forEach(IntermediateAggregates::commit)

            intermediateAggregates.clear()
            aggregateMap.clear()

            logger.debug { "Aggregation commit completed." }
        }
    }
}

/**
 * Represents a set of intermediate aggregates for a specific (project, version)
 * pair. Stored in the central aggregation manager.
 */
private data class IntermediateAggregates(val project: String, val version: String) {
    private val aggregates: MutableMap<String, MutableMap<Int, IntermediateAggregate>> = mutableMapOf()

    /**
     * Aggregates the specified LogEntry in this intermediate aggregation.
     */
    fun aggregate(entry: LogEntry) {
        val fileAggregates = aggregates.getOrPut(entry.location.file, ::mutableMapOf)
        val lineAggregates = fileAggregates.getOrPut(entry.location.line, { IntermediateAggregate(entry.severity, 0) })

        // Warn if the two do not match in severity.
        if (lineAggregates.severity != entry.severity) {
            logger.warn {
                "Existing severity of ${entry.location.file}:${entry.location.line} (${lineAggregates.severity}) does not match new severity: ${entry.severity}"
            }
        }

        lineAggregates.count += 1
    }

    /**
     * Commits this specific aggregate container to the database synchronously.
     */
    fun commit() {
        val entries = aggregates.flatMap { (file, fileEntries) ->
            fileEntries.map { (line, lineAggregate) ->
                AggregateInsertContainer(project, version, lineAggregate.severity, file, line, lineAggregate.count)
            }
        }

        // Insert in batch.
        transaction {
            AggregationEntries.batchInsert(entries) {
                this[AggregationEntries.timestamp] = DateTime.now()
                this[AggregationEntries.project] = it.project
                this[AggregationEntries.version] = it.version
                this[AggregationEntries.severity] = it.severity
                this[AggregationEntries.file] = it.file
                this[AggregationEntries.line] = it.line
                this[AggregationEntries.numTriggers] = it.numTriggers
            }
        }
    }
}

/**
 * Represents an aggregate for a (project, version, file, line) pair. Stores
 * the severity, but this value is not used in the categorization (i.e. if
 * two logs on the same line differ in severity, a warning will be logged
 * and the first one encountered will be the value persisted).
 */
private data class IntermediateAggregate(val severity: String, var count: Int)

/**
 * Intermediate container for efficiently inserting all entries in the database
 * in a single batch call. Corresponds to the fields declared in
 * @see AggregationEntries
 */
private data class AggregateInsertContainer(
        val project: String,
        val version: String,
        val severity: String,
        val file: String,
        val line: Int,
        val numTriggers: Int
)