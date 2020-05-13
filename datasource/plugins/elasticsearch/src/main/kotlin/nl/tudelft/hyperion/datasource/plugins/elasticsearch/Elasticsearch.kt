package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import mu.KotlinLogging
import nl.tudelft.hyperion.datasource.common.DataSourcePlugin
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.builder.SearchSourceBuilder
import java.util.*
import kotlin.concurrent.fixedRateTimer

/**
 * Plugin that periodically queries an Elasticsearch instance.
 * Starts a daemon that sends a search query every [Configuration.pollInterval].
 *
 * @property config the configuration to use
 * @property esClient Elasticsearch client for requests
 * @property redis Redis client for publishing to
 *  it is assumed to be correct, otherwise exceptions can be thrown
 */
class Elasticsearch(
        private var config: Configuration,
        val esClient: RestHighLevelClient,
        private val redis: RedisClient
) : DataSourcePlugin {

    private var finished = false
    private val channelConfig = "${config.name}${config.registrationChannelPostfix}"
    private var timer: Timer? = null
    private var pubChannel: String? = null
    var publisherConn: StatefulRedisPubSubConnection<String, String>? = null
    var publisher: RedisAsyncCommands<String, String>? = null

    companion object {
        private val logger = KotlinLogging.logger {}

        /**
         * Creates a search request that queries all logs between a certain timestamp.
         *
         * @param index the name of the ES index to query from
         * @param timeStampField the name of the time field in the ES index
         * @param currentTime the current epoch time in seconds
         * @param range the time range in seconds
         * @param responseHitCount the maximum amount of hits to return
         * @return the created search request
         */
        fun createSearchRequest(
                index: String,
                timeStampField: String,
                currentTime: Int,
                range: Int,
                responseHitCount: Int): SearchRequest {

            val searchRequest = SearchRequest()
            searchRequest.indices(index)

            val query = QueryBuilders
                    .rangeQuery(timeStampField)
                    .gt(currentTime - range)
                    .to(currentTime)
                    .format("epoch_second")

            logger.debug { "Sending query: $query" }

            val searchBuilder = SearchSourceBuilder()
            searchBuilder.query(query)
            searchBuilder.size(responseHitCount)

            return searchRequest.source(searchBuilder)
        }

        /**
         * Builder for the Elasticsearch plugin.
         *
         * @param config Configuration to build from
         * @return Elasticsearch object
         */
        fun build(config: Configuration): Elasticsearch {
            if (config.es.responseHitCount >= 10_000) {
                logger.warn {
                    """
                response_hit_max=${config.es.responseHitCount}, by default this value is capped at 10.000.
                Elasticsearch will deny requests if index.max_result_window is not configured.
                """.trimIndent()
                }
            }

            // create Redis client
            val redis = RedisClient.create(RedisURI.create(config.redis.host, config.redis.port!!))

            // create Elasticsearch client
            val clientBuilder = RestClient.builder(HttpHost(
                    config.es.hostname,
                    config.es.port!!,
                    config.es.scheme!!))

            // add credentials to httpClient if authentication is enabled
            if (config.es.authentication) {
                clientBuilder.addAuthentication(config.es.username!!, config.es.password!!)

                logger.info { "Elasticsearch authentication enabled" }
            }

            val client = RestHighLevelClient(clientBuilder)

            logger.info { "Elasticsearch client created successfully" }

            return Elasticsearch(config, client, redis)
        }

        /**
         * Modifies the http client to include authentication for requests.
         *
         * @param username Elasticsearch username
         * @param password Elasticsearch password
         */
        private fun RestClientBuilder.addAuthentication(username: String, password: String) {
            val credentialsProvider: CredentialsProvider = BasicCredentialsProvider()

            credentialsProvider.setCredentials(AuthScope.ANY,
                    UsernamePasswordCredentials(username, password))

            this.setHttpClientConfigCallback { httpClientBuilder ->
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            }
        }
    }

    /**
     * Passes a searchHit to the assigned Redis pub channel.
     *
     * @param searchHit the searchHit to send in JSON
     */
    fun sendHit(searchHit: SearchHit) {
        publisher?.publish(pubChannel, searchHit.sourceAsString)
    }

    override fun start() {
        if (finished)
            throw IllegalStateException("Elasticsearch client is already closed")

        logger.info { "Starting Redis client" }

        pubChannel = redis.connect().sync().hget(channelConfig, "pubChannel")
                ?: throw IllegalStateException("pubChannel not set in $channelConfig")

        logger.info { "Publishing to channel: $pubChannel" }

        publisherConn = redis.connectPubSub()
        publisher = publisherConn!!.async()

        val requestHandler = RequestHandler(::sendHit)

        logger.info { "Starting retrieval of logs" }

        timer = fixedRateTimer("requestScheduler", period = config.pollInterval * 1000.toLong(), daemon = true) {
            val searchRequest = createSearchRequest(config.es.index,
                    config.es.timestampField,
                    (System.currentTimeMillis() / 1000).toInt(),
                    config.pollInterval,
                    config.es.responseHitCount)

            esClient.searchAsync(searchRequest, RequestOptions.DEFAULT, requestHandler)
        }
    }

    override fun stop() {
        timer?.cancel() ?: logger.warn { "Stop called on Elasticsearch plugin before initialization" }
    }

    override fun cleanup() {
        logger.info { "Elasticsearch plugin closed" }
        esClient.close()

        logger.info { "Closing Redis pub/sub connection" }
        publisherConn?.flushCommands()
        publisherConn?.close()
        redis.shutdown()

        finished = true
    }
}
