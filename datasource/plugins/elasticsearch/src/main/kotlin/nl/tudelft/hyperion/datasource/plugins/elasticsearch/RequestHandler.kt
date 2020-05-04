package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import mu.KotlinLogging
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.search.SearchResponse
import java.io.PrintWriter
import java.io.StringWriter

private val logger = KotlinLogging.logger {}

/**
 * Class that transforms responses from Elasticsearch and sends them to a broker.
 */
class RequestHandler : ActionListener<SearchResponse> {

    /**
     * Passes the response as a serialized JSON string to the broker.
     *
     * @param response [SearchResponse] object to send
     */
    override fun onResponse(response: SearchResponse?) {
        TODO("Not yet implemented")
    }

    /**
     * Logs the exception but does not handle it.
     *
     * @param e [Exception] caused by the sent request
     */
    override fun onFailure(e: Exception?) {
        val sw = StringWriter()
        e!!.printStackTrace(PrintWriter(sw))
        logger.error { "Error during handling of Elasticsearch response: \n$sw" }
    }
}