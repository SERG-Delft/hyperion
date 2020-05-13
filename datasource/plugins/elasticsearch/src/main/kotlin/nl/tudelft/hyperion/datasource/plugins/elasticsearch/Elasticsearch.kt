package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import mu.KotlinLogging
import nl.tudelft.hyperion.datasource.common.DataPluginInitializationException
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
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.zeromq.SocketType
import org.zeromq.ZContext
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.fixedRateTimer

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
) : DataSourcePlugin {

    private var finished = false
    private var hasConnectionInformation = false
    private lateinit var timer: Timer
    private lateinit var pubConnectionInformation: PeerConnectionInformation

    private val senderScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    private val queue = Channel<String>(20_000)

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

            credentialsProvider.setCredentials(AuthScope.ANY,
                    UsernamePasswordCredentials(username, password))

            this.setHttpClientConfigCallback { httpClientBuilder ->
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            }
        }
    }

    /**
     * Synchronously attempts to query the connection information from the plugin
     * manager. This will block the calling thread until a response is received
     * from the plugin manager, which may take an arbitrary amount of time depending
     * on whether the service is online.
     */
    fun queryConnectionInformation() {
        ZContext().use {
            val socket = it.createSocket(SocketType.REQ)
            socket.connect("tcp://${config.pluginManager.address}")

            socket.send("""{"id":"${config.id}","type":"out"}""")
            pubConnectionInformation = Utils.readJSONContent(socket.recvStr())

            logger.debug { "pubConnectionInformation: $pubConnectionInformation" }
        }

        logger.debug { "Successfully retrieved connection information" }

        hasConnectionInformation = true
    }

    /**
     * Helper function that will create a new subroutine that is used to send the
     * results of computation to the next stage in the pipeline.
     */
    private fun runSender(channel: Channel<String>) = senderScope.launch {
        val ctx = ZContext()
        val sock = ctx.createSocket(SocketType.PUSH)

        if (pubConnectionInformation.isBind) {
            logger.debug { "Binding socket to ${pubConnectionInformation.host}" }
            sock.bind(pubConnectionInformation.host)
        } else {
            logger.debug { "Connecting socket to ${pubConnectionInformation.host}" }
            sock.connect(pubConnectionInformation.host)
        }

        while (isActive) {
            sock.send(channel.receive(), zmq.ZMQ.ZMQ_DONTWAIT)
        }

        sock.close()
        ctx.destroy()
    }

    override fun start() = GlobalScope.launch {
        if (finished) {
            throw IllegalStateException("Elasticsearch client is already closed")
        }

        if (!hasConnectionInformation) {
            throw DataPluginInitializationException("Connection information is not set")
        }

        val requestHandler = RequestHandler {
            runBlocking {
                queue.send(it.sourceAsString)
            }
        }

        val sender = runSender(queue)

        logger.info { "Starting retrieval of logs" }

        timer = fixedRateTimer("requestScheduler", period = config.pollInterval * 1000.toLong(), daemon = true) {
            val searchRequest = createSearchRequest(config.es.index,
                    config.es.timestampField,
                    (System.currentTimeMillis() / 1000).toInt(),
                    config.pollInterval,
                    config.es.responseHitCount)

            esClient.searchAsync(searchRequest, RequestOptions.DEFAULT, requestHandler)
        }

        try {
            while (isActive) {
                delay(Long.MAX_VALUE)
            }
        } finally {
            this@Elasticsearch.stop()
            this@Elasticsearch.cleanup()
            sender.cancelAndJoin()
        }
    }

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
}

/**
 * Represents the information needed for this plugin to connect to any
 * future elements in the pipeline. Contains the host, port and whether
 * it needs to bind or connect to that specific element.
 */
private data class PeerConnectionInformation(
        val host: String,
        val isBind: Boolean
)
