package nl.tudelft.hyperion.datasource.common

/**
 * Configuration for Redis communication.
 *
 * @property host hostname of the Redis instance to publish to
 * @property port port of the Redis instance to publish to
 * @property channel name of the Redis channel to publish to
 */
data class RedisConfig(
        val host: String,
        var port: Int?,
        var channel: String?
) {
    init {
        // set default values if field is missing
        // jackson does not support default value setting as of 2.7.1
        if (port == null) {
            port = 6379
        }
    }
}
