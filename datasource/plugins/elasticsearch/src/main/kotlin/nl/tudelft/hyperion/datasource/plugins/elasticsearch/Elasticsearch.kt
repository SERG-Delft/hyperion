package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import mu.KotlinLogging
import nl.tudelft.hyperion.datasource.common.DataSourcePlugin
import org.apache.http.HttpHost
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import java.lang.IllegalStateException
import java.util.*
import kotlin.concurrent.fixedRateTimer

private val logger = KotlinLogging.logger {}

/**
 * Plugin that periodically queries an Elasticsearch instance.
 * Starts a daemon that sends a search query every [pollInterval].
 *
 * @param hostname the hostname of the ES instance
 * @param port which port the ES instance is located on
 * @param scheme which scheme to use, should be http or https
 * @property index the name of the index to query
 * @property pollInterval the interval between requests in milliseconds
 * @property responseHitMax the maximum amount of hits to return, ideally all
 *  matches should be returned. But this is limited to index.max_result_window,
 *  which is 10K by default
 */
class Elasticsearch(
        hostname: String,
        port: Int = 9200,
        scheme: String = "http",
        private val index: String,
        private val pollInterval: Long = 1000L,
        private val responseHitMax: Int = 10_000
) : DataSourcePlugin {

    private val client = RestHighLevelClient(RestClient.builder(HttpHost(hostname, port, scheme)))
    private var timer: Timer? = null
    private var finished = false

    companion object {
        private val requestHandler = RequestHandler()
    }

    override fun start() {
        if (finished)
            throw IllegalStateException("Elasticsearch client is already closed")

        // Create a new handler for each request
        timer = fixedRateTimer("requestScheduler", period = pollInterval, daemon = true) {
            val searchRequest = createSearchRequest(index)
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

    /**
     * Creates a search request that queries all logs between a certain timestamp.
     *
     * @param index the name of the ES index to query from
     * @return the created search request
     */
    private fun createSearchRequest(index: String): SearchRequest {
        val searchRequest = SearchRequest()
        searchRequest.indices(index)

        val searchSourceBuilder = SearchSourceBuilder()
        searchSourceBuilder.query(QueryBuilders.matchAllQuery())
        searchSourceBuilder.size(responseHitMax)

        return searchRequest.source(searchSourceBuilder)
    }
}
