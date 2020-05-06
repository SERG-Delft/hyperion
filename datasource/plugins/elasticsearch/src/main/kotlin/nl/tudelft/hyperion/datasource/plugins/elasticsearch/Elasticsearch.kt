package nl.tudelft.hyperion.datasource.plugins.elasticsearch

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
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import redis.clients.jedis.Jedis
import java.util.*
import kotlin.concurrent.fixedRateTimer

/**
 * Plugin that periodically queries an Elasticsearch instance.
 * Starts a daemon that sends a search query every [Configuration.pollInterval].
 *
 * @property config the configuration to use
 *  it is assumed to be correct, otherwise exceptions can be thrown
 */
class Elasticsearch(private val config: Configuration) : DataSourcePlugin {

    private var finished = false
    private var timer: Timer? = null
    private var client: RestHighLevelClient

    init {
        if (config.es.responseHitCount >= 10_000) {
            logger.warn {
                """
                response_hit_max=$config.responseHitMax, by default this value is capped at 10.000. 
                Elasticsearch will deny requests if index.max_result_window is not configured.
                """.trimIndent()
            }
        }

        val clientBuilder = RestClient.builder(HttpHost(
                config.es.hostname,
                config.es.port!!,
                config.es.scheme!!))

        // add credentials to httpClient if authentication is enabled
        if (config.es.authentication) {
            val credentialsProvider: CredentialsProvider = BasicCredentialsProvider()

            credentialsProvider.setCredentials(AuthScope.ANY,
                    UsernamePasswordCredentials(config.es.username!!, config.es.password!!))

            clientBuilder.setHttpClientConfigCallback { httpClientBuilder ->
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            }
        }

        this.client = RestHighLevelClient(clientBuilder)

        logger.info { "Elasticsearch client created successfully" }
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        /**
         * Creates a search request that queries all logs between a certain timestamp.
         *
         * @param index the name of the ES index to query from
         * @return the created search request
         */
        private fun createSearchRequest(
                index: String,
                timeStampField: String,
                currentTime: Long,
                range: Int,
                responseHitCount: Int): SearchRequest {

            val searchRequest = SearchRequest()
            searchRequest.indices(index)

            val query = QueryBuilders
                    .rangeQuery(timeStampField)
                    .from(currentTime - range)
                    .to(currentTime)
                    .format("epoch_second")

            val searchBuilder = SearchSourceBuilder()
            searchBuilder.query(query)
            searchBuilder.size(responseHitCount)

            return searchRequest.source(searchBuilder)
        }
    }

    override fun start() {
        if (finished)
            throw IllegalStateException("Elasticsearch client is already closed")

        logger.info { "Starting Redis client" }

        val jedis = Jedis(config.redis.host, config.redis.port!!)
        val requestHandler = RequestHandler(jedis, config.redis.channel!!)

        logger.info { "Starting retrieval of logs" }

        timer = fixedRateTimer("requestScheduler", period = config.pollInterval.toLong() * 1000, daemon = true) {
            val searchRequest = createSearchRequest(config.es.index,
                    config.es.timestampField,
                    System.currentTimeMillis() / 1000,
                    config.pollInterval,
                    config.es.responseHitCount)

            client.searchAsync(searchRequest, RequestOptions.DEFAULT, requestHandler)
        }
    }

    override fun stop() {
        timer?.cancel() ?: logger.warn { "Stop called on Elasticsearch plugin before initialization" }
    }

    override fun cleanup() {
        logger.info { "Elasticsearch plugin closed" }
        client.close()
        finished = true
    }
}
