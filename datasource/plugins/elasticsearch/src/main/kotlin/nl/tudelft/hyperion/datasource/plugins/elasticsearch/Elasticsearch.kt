package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import nl.tudelft.hyperion.datasource.common.DataPluginInitializationException
import nl.tudelft.hyperion.datasource.common.DataSourcePlugin
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
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
import org.elasticsearch.search.builder.SearchSourceBuilder
import java.util.Timer
import kotlin.concurrent.fixedRateTimer
import kotlin.coroutines.CoroutineContext

/**
 * Plugin that periodically queries an Elasticsearch instance.
 * Starts a daemon that sends a search query every [Configuration.pollInterval].
 *
 * @property config the configuration to use
 * @property esClient Elasticsearch client for requests
 */
class Elasticsearch(
    private var config: Configuration,
    val esClient: RestHighLevelClient
) : AbstractPipelinePlugin(config.pipeline), DataSourcePlugin {

    private var finished = false
    lateinit var timer: Timer

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
            responseHitCount: Int
        ): SearchRequest {

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

            // create Elasticsearch client
            val clientBuilder = RestClient.builder(
                HttpHost(
                    config.es.hostname,
                    config.es.port,
                    config.es.scheme
                )
            )

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
        private fun RestClientBuilder.addAuthentication(username: String, password: String) {
            val credentialsProvider: CredentialsProvider = BasicCredentialsProvider()

            credentialsProvider.setCredentials(
                AuthScope.ANY,
                UsernamePasswordCredentials(username, password)
            )

            this.setHttpClientConfigCallback { httpClientBuilder ->
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            }
        }
    }

    /**
     * Starts a coroutine in the global scope which in turn starts
     * a [java.util.Timer] that periodically schedules a request to
     * Elasticsearch and a worker that sends the documents to a ZMQ
     * Socket.
     *
     * @param context additional coroutine context to add
     */
    override fun run(context: CoroutineContext): Job = GlobalScope.launch(context) {
        if (finished) {
            throw IllegalStateException("Elasticsearch client is already closed")
        }

        if (!canSend) {
            throw DataPluginInitializationException("Data source must be the first step in the pipeline.")
        }

        val requestHandler = RequestHandler {
            sendQueued(it.sourceAsString)
        }

        val pipelineWorker = super.run(context)

        logger.info { "Starting retrieval of logs" }

        timer = fixedRateTimer("requestScheduler", period = config.pollInterval * 1000.toLong(), daemon = true) {
            val searchRequest = createSearchRequest(
                config.es.index,
                config.es.timestampField,
                (System.currentTimeMillis() / 1000).toInt(),
                config.pollInterval,
                config.es.responseHitCount
            )

            esClient.searchAsync(searchRequest, RequestOptions.DEFAULT, requestHandler)
        }

        try {
            while (isActive) {
                delay(Long.MAX_VALUE)
            }
        } finally {
            this@Elasticsearch.stop()
            this@Elasticsearch.cleanup()
            pipelineWorker.cancelAndJoin()
        }
    }

    override fun start(): Job = run(Dispatchers.Default)

    override fun stop() {
        if (this::timer.isInitialized) {
            timer.cancel()
        } else {
            logger.warn { "Stop called on Elasticsearch plugin before initialization" }
        }
    }

    override fun cleanup() {
        logger.info { "Closing Elasticsearch client" }
        esClient.close()

        finished = true
    }

    override suspend fun onMessageReceived(msg: String) {
        // Should never receive a message.
    }
}
