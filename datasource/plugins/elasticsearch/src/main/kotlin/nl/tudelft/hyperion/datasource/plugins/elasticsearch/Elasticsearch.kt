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

/**
 * Plugin that periodically queries an Elasticsearch instance.
 * Starts a daemon that sends a search query every [Configuration.pollInterval].
 *
 * @property config the configuration to use
 */
class Elasticsearch(private val config: Configuration) : DataSourcePlugin {

    private var finished = false
    private var timer: Timer? = null
    private val client = RestHighLevelClient(RestClient.builder(HttpHost(
            config.hostname,
            config.port!!,
            config.scheme!!)))

    init {
        if (config.responseHitCount >= 10_000) {
            logger.warn {
                """
                response_hit_max=$config.responseHitMax, by default this value is capped at 10.000. 
                Elasticsearch will deny requests if index.max_result_window is not configured.
                """.trimIndent()
            }
        }
    }

    companion object {
        private val requestHandler = RequestHandler()
        private val logger = KotlinLogging.logger {}
    }

    override fun start() {
        if (finished)
            throw IllegalStateException("Elasticsearch client is already closed")

        timer = fixedRateTimer("requestScheduler", period = config.pollInterval, daemon = true) {
            val searchRequest = createSearchRequest(config.index)
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
        searchSourceBuilder.size(config.responseHitCount)

        return searchRequest.source(searchSourceBuilder)
    }
}
