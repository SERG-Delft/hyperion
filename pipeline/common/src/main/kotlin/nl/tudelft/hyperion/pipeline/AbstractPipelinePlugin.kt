package nl.tudelft.hyperion.pipeline

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.pubsub.RedisPubSubAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Represents an abstract pipeline plugin that receives some JSON
 * representation of a value and asynchronously transforms it into
 * some other representation. All communication is handled by this
 * class, such that implementors can focus on doing the actual
 * transformation.
 */
abstract class AbstractPipelinePlugin(
    private val config: PipelinePluginConfiguration
) {
    private val logger = mu.KotlinLogging.logger {}
    private val pluginThreadPool = CoroutineScope(
        Executors
            .newFixedThreadPool(4)
            .asCoroutineDispatcher()
    )

    /**
     * Connects to redis and registers a pub/sub listener for this plugin. Returns
     * a job that, when cancelled, will unregister from redis and return. Otherwise,
     * this job will run infinitely.
     *
     * Will throw if anything goes wrong during initialization.
     */
    public fun start() = GlobalScope.launch {
        val connection = try {
            RedisClient
                .create()
                .connectAsync(
                    StringCodec.UTF8,
                    RedisURI.create(config.redis.host, config.redis.port)
                )
                .await()
        } catch (ex: Exception) {
            throw PipelinePluginInitializationException("Could not connect to redis", ex)
        }

        val commands = connection.async()
        val isSubscriber = commands.queryPluginConfig("subscriber") == "true"
        val isPublisher = commands.queryPluginConfig("publisher") == "true"
        val subChannel = commands.queryPluginConfig("subChannel")
        val pubChannel = commands.queryPluginConfig("pubChannel")

        logger.info {
            "Initializing ${config.id}, subscribing to $subChannel and publishing to $pubChannel"
        }

        if (!isSubscriber || !isPublisher) {
            throw PipelinePluginInitializationException(
                "Pipeline plugins inheriting from AbstractPipelinePlugin must be both a subscriber and a publisher"
            )
        }

        val pubSubConnection = try {
            RedisClient
                .create()
                .connectPubSubAsync(
                    StringCodec.UTF8,
                    RedisURI.create(config.redis.host, config.redis.port)
                )
                .await()
        } catch (ex: Exception) {
            throw PipelinePluginInitializationException("Could not connect to redis", ex)
        }

        val pubSubCommands = pubSubConnection.async()
        pubSubCommands.subscribe(subChannel).await()

        val listener = createPubSubListener(commands, pubChannel)
        pubSubConnection.addListener(listener)

        logger.info {
            "Plugin ${config.id} is running!"
        }

        try {
            while (isActive) {
                delay(Long.MAX_VALUE)
            }
        } finally {
            pubSubCommands.unsubscribe(subChannel)
            pubSubConnection.removeListener(listener)
        }
    }

    /**
     * Method that performs the plugin transform on the specified input.
     * This method can suspend, thus it can perform asynchronous transformations
     * before returning a result.
     *
     * If this method throws, its result is discarded. Since this will cause
     * data loss, it is recommended to only throw if no other options exist.
     * Returning null will also discard the result, but without the performance
     * cost of throwing/handling an exception.
     */
    abstract suspend fun process(input: String): String?

    /**
     * Helper function that creates an anonymous pub-sub adapter that runs
     * process on the incoming messages and sends them to the specified outChannel.
     */
    private fun createPubSubListener(
        commands: RedisAsyncCommands<String, String>,
        outChannel: String
    ) = object : RedisPubSubAdapter<String, String>() {
        override fun message(channel: String?, message: String?) {
            pluginThreadPool.launch {
                val result = try {
                    this@AbstractPipelinePlugin.process(message!!)
                } catch (ex: Exception) {
                    logger.warn(ex) { "Error processing message: '$message'" }
                    null
                } ?: return@launch

                commands.publish(outChannel, result)
            }
        }
    }

    /**
     * Helper function that will query the specified config key for this plugin from redis.
     * Will throw if the property is not present.
     */
    private suspend fun RedisAsyncCommands<String, String>.queryPluginConfig(path: String) = run {
        this
            .hget(config.id, path)
            .await() ?: throw PipelinePluginInitializationException("Required property '$path' is missing in redis")
    }
}

/**
 * Exception thrown during initialization of a pipeline plugin.
 */
private class PipelinePluginInitializationException(
    msg: String,
    cause: Throwable? = null
) : RuntimeException(msg, cause)
