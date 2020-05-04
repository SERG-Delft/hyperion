@file:JvmName("Main")

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
 * Elasticsearch plugin that periodically queries a database.
 *
 * @param hostname
 * @param index
 * @param port
 * @param scheme
 * @property pollInterval
 * @property responseHitMax the maximum amount of hits to return, ideally all
 *  matches should be returned. But this is limited to index.max_result_window,
 *  which is 10K by default
 */
class Elasticsearch(
        hostname: String,
        index: String,
        port: Int = 9200,
        scheme: String = "http",
        private val pollInterval: Long = 1000L,
        private val responseHitMax: Int = 10_000
) : DataSourcePlugin {

    private val client = RestHighLevelClient(RestClient.builder(HttpHost(hostname, port, scheme)))
    private val searchRequest = createSearchRequest(index)
    private var timer: Timer? = null
    private var finished = false

    override fun start() {
        if (finished)
            throw IllegalStateException("Elasticsearch client is already closed")

        // Create a new handler for each request
        timer = fixedRateTimer("requestScheduler", period = pollInterval, daemon = true) {
            client.searchAsync(searchRequest, RequestOptions.DEFAULT, RequestHandler())
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
     * Creates the default search request.
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

fun main() {
}
