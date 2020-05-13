package nl.tudelft.hyperion.plugin.metric

/**
 * Represents a single grouped metric of a (project, file, line, version,
 * severity) tuple. Contains the line for which it was triggered, the
 * severity of the log and the amount of times that specific log was
 * triggered.
 */
data class Metric(
        var line: Int,
        val count: Int,
        val severity: Severity
)