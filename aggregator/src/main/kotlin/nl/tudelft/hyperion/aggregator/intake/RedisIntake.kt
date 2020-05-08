package nl.tudelft.hyperion.aggregator.intake

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.pubsub.RedisPubSubListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.aggregator.RedisConfiguration
import nl.tudelft.hyperion.aggregator.api.LogEntry
import nl.tudelft.hyperion.aggregator.workers.AggregationManager

// TODO: Make this configurable
private const val CONFIG_PATH = "Aggregator-config"

/**
 * Central class that manages intake from the Redis command channel. Will
 * receive messages from the pipeline, forwarding them to the aggregation
 * manager for eventual aggregation.
 */
class RedisIntake(
    val configuration: RedisConfiguration,
    val aggregationManager: AggregationManager
) : RedisPubSubListener<String, String> {
    private val logger = mu.KotlinLogging.logger {}
    private val uri = RedisURI.create("redis://${configuration.host}:${configuration.port}/0")
    private val redisClient = RedisClient.create(uri)

    /**
     * Connects to the redis client, queries what channels it needs to operate on,
     * then sets up a listener for that specific channel, forwarding messages to
     * the aggregation manager. Will throw if anything goes wrong during initialization.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun setup() {
        try {

            logger.info { "Connecting to redis at $uri..." }

            val connection = redisClient.connectAsync(
                StringCodec.UTF8,
                uri
            ).await()

            val commands = connection.async()
            val channel = commands.hget(CONFIG_PATH, "subChannel").await()
                ?: throw RedisIntakeInitializationException(
                    "No subscriber channel defined for aggregator. Is your plugin manager configured properly?"
                )

            setupPubSubListener(channel)
        } catch (ex: Exception) {
            throw RedisIntakeInitializationException("Failed to setup redis intake", ex)
        }
    }

    /**
     * Connects to the redis client and configures the redis intake to listen
     * to the specified channel and handle any incoming messages as log entries.
     */
    suspend fun setupPubSubListener(channel: String) {
        logger.info { "Will listen to log entries sent in '$channel'." }

        val pubsubConnection = redisClient.connectPubSubAsync(
            StringCodec.UTF8,
            uri
        ).await()
        pubsubConnection.addListener(this)

        val pubsubCommands = pubsubConnection.async()
        pubsubCommands.subscribe(channel).await()
    }

    /**
     * Called by redis when we receive a new message. We will ignore the channel,
     * and instead assume that any message on a channel that we've subscribed to
     * contains valid log entries.
     */
    @Suppress("TooGenericExceptionCaught")
    override fun message(channel: String, message: String) {
        logger.debug { "Received message from redis: '$message'" }

        val entry = try {
            LogEntry.parse(message)
        } catch (ex: Exception) {
            logger.warn(ex) {
                "Failed to parse incoming log entry. Are you sure that your pipeline is configured properly?"
            }

            return
        }

        // Aggregate asynchronously.
        GlobalScope.launch(Dispatchers.Default) {
            aggregationManager.aggregate(entry)
        }
    }

    // <editor-fold desc="Redis pub-sub listener unused callbacks">
    override fun psubscribed(pattern: String?, count: Long) {
        // Do nothing
    }

    override fun punsubscribed(pattern: String?, count: Long) {
        // Do nothing
    }

    override fun unsubscribed(channel: String?, count: Long) {
        // Do nothing
    }

    override fun subscribed(channel: String?, count: Long) {
        // Do nothing
    }

    override fun message(pattern: String?, channel: String?, message: String?) {
        // Do nothing
    }
    // </editor-fold>
}

/**
 * Represents that an error was encountered during initialization of the redis intake service.
 */
class RedisIntakeInitializationException(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause)
