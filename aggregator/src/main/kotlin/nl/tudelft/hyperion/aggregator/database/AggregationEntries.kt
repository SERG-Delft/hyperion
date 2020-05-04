package nl.tudelft.hyperion.aggregator.database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.jodatime.datetime

/**
 * Represents the database table that contains an aggregation entry. An
 * aggregation entry is a single sum of log entries over a given timeframe
 * as specified by the granularity setting of the configuration. By summing
 * intermediate aggregated entries, an aggregation for variable timeframes
 * can be computed.
 */
object AggregationEntries : Table() {
    override val tableName = "aggregation_entries"

    /**
     * The timestamp of this aggregation. This is the time at which the aggregation
     * is completed, i.e. the time at which the entry is inserted in the database.
     */
    val timestamp = datetime("timestamp")

    /**
     * The project/codebase this aggregation entry belongs to. This is to identify
     * different code bases that may have similar file names.
     */
    val project = text("project")

    /**
     * The version of the code that generated this log line. This is a freeform field,
     * but is usually represented as a git commit hash/tag name.
     */
    val version = text("version")

    /**
     * The full path relative to the repository root where the log line was generated.
     */
    val file = text("file")

    /**
     * The line on which the log line was generated.
     */
    val line = integer("line")

    /**
     * The severity of the log line. Free-form, but usually one of the default log levels.
     */
    val severity = text("severity")

    /**
     * The amount of triggers this specific combination of project-version-file-name-severity
     * has had over a time period of `Configuration.granularity` seconds long.
     */
    val numTriggers = integer("num_triggers")

    // Index for speeding up queries.
    val projectFileIdx = index("aggregation_entries_idx_project_file", false, project, file)
}
