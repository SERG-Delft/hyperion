package nl.tudelft.hyperion.aggregator.intake

import kotlinx.coroutines.Job
import nl.tudelft.hyperion.aggregator.Configuration
import nl.tudelft.hyperion.aggregator.api.LogEntry
import nl.tudelft.hyperion.aggregator.workers.AggregationManager
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import org.joda.time.DateTime
import org.joda.time.Seconds
import kotlin.coroutines.CoroutineContext

/**
 * Central class that manages intake from the ZeroMQ command channel. Will
 * receive messages from the pipeline, forwarding them to the aggregation
 * manager for eventual aggregation.
 */
class ZMQIntake(
    private val pluginConfiguration: Configuration,
    private val aggregationManager: AggregationManager,
    configuration: PipelinePluginConfiguration = pluginConfiguration.pipeline
) : AbstractPipelinePlugin(configuration) {
    override val logger = mu.KotlinLogging.logger {}

    // Ensure that we're a receiver.
    override fun run(context: CoroutineContext): Job {
        if (!canReceive) {
            throw IllegalStateException(
                "Cannot run ZMQ intake if it cannot receive. Ensure that the aggregator is the last step in the " +
                    "pipeline."
            )
        }

        return super.run(context)
    }

    // Handle messages coming form the pipeline.
    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    override suspend fun onMessageReceived(msg: String) {
        logger.debug { "Received message from ZMQ: '$msg'" }

        val entry = try {
            LogEntry.parse(msg)
        } catch (ex: Exception) {
            logger.warn(ex) {
                "Failed to parse incoming log entry. Are you sure that your pipeline is configured properly?"
            }

            return
        }

        if (pluginConfiguration.verifyTimestamp) {
            if (entry.timestamp == null) {
                logger.warn {
                    "Received a log entry with a missing timestamp. Cannot verify that the entry fits " +
                        "within the current time period, and will therefore ignore it. You can disable this " +
                        "behavior by setting the `verify-timestamp` property to false in the config."
                }

                return
            }

            val secondsSinceLogEntry = Seconds.secondsBetween(entry.timestamp, DateTime.now()).seconds
            if (secondsSinceLogEntry > pluginConfiguration.granularity) {
                logger.warn {
                    "Received a log entry that happened $secondsSinceLogEntry seconds ago, while the " +
                        "granularity of the aggregator is set to ${pluginConfiguration.granularity} seconds. " +
                        "Will ignore this log entry. You can fix this warning by either increasing your " +
                        "granularity, decreasing the time between log and it arriving at the aggregator, " +
                        "or by setting the `verify-timestamp` to false in the configuration."
                }

                return
            }
        }

        aggregationManager.aggregate(entry)
    }
}
