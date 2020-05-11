package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import mu.KotlinLogging
import nl.tudelft.hyperion.datasource.common.DataSourcePlugin
import nl.tudelft.hyperion.pluginmanager.hyperionplugin.HyperionPlugin
import nl.tudelft.hyperion.pluginmanager.hyperionplugin.PluginConfiguration
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
 * @constructor
 * Takes the fields from the configuration format defined in this plugin
 * and creates a [nl.tudelft.hyperion.pluginmanager.hyperionplugin.PluginConfiguration]
 *
 * @property config the configuration to use
 *  it is assumed to be correct, otherwise exceptions can be thrown
 */
class Elasticsearch(_pluginConfig: PluginConfiguration) : HyperionPlugin(_pluginConfig), DataSourcePlugin {

    lateinit var config: Configuration
    lateinit var client: RestHighLevelClient
    private var finished = false
    private var timer: Timer? = null

    constructor(
            config: Configuration,
            client: RestHighLevelClient
    ) : this(PluginConfiguration(config.redis, config.registrationChannelPostfix, config.name)) {
        this.config = config
        this.client = client
    }

    companion object {
        private val logger = KotlinLogging.logger {}

        /**
         * Creates a search request that queries all logs between a certain timestamp.
         *
         * @param index the name of the ES index to query from
         * @return the created search request
         */
        fun createSearchRequest(
                index: String,
                timeStampField: String,
                currentTime: Long,
                range: Int,
                responseHitCount: Int): SearchRequest {

            val searchRequest = SearchRequest()
            searchRequest.indices(index)

            val query = QueryBuilders
                    .rangeQuery(timeStampField)
                    .gt(currentTime - range)
                    .to(currentTime)
                    .format("epoch_second")

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
                clientBuilder.addAuthentication(config.es.username!!, config.es.password!!)

                logger.info { "Elasticsearch authentication enabled" }
            }

            val client = RestHighLevelClient(clientBuilder)

            logger.info { "Elasticsearch client created successfully" }

            return Elasticsearch(config, client)
        }

        /**
         * Modifies the http client to include authentication for requests.
         *
         * @param username Elasticsearch username
         * @param password Elasticsearch password
         */
        fun RestClientBuilder.addAuthentication(username: String, password: String) {
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
    private fun sendHit(searchHit: SearchHit) {
        val sync = this.pub.sync()
        sync.publish(pubChannel, searchHit.sourceAsString)
    }

    override fun start() {
        if (finished)
            throw IllegalStateException("Elasticsearch client is already closed")

        logger.info { "Starting Redis client" }

        val requestHandler = RequestHandler(::sendHit)

        logger.info { "Starting retrieval of logs" }

        timer = fixedRateTimer("requestScheduler", period = config.pollInterval * 1000.toLong(), daemon = true) {
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

        logger.info { "Closing Redis pub/sub connection" }
        pub.flushCommands()
        pub.close()

        finished = true
    }

    override fun work(message: String): String {
        throw IllegalStateException("work should not be called on Elasticsearch plugin")
    }
}
