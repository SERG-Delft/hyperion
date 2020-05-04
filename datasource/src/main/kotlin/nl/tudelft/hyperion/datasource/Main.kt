@file:JvmName("Main")

package nl.tudelft.hyperion.datasource

import kotlinx.coroutines.runBlocking
import org.apache.http.HttpHost
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import kotlin.concurrent.fixedRateTimer

fun createRequest(requestSize: Int): SearchRequest {
    val searchRequest = SearchRequest()
    searchRequest.indices("logs")

    val searchSourceBuilder = SearchSourceBuilder()
    searchSourceBuilder.query(QueryBuilders.matchAllQuery())
    searchSourceBuilder.size(requestSize)

    return searchRequest.source(searchSourceBuilder)
}

class Listener : ActionListener<SearchResponse> {
    override fun onResponse(response: SearchResponse?) = runBlocking {
        println("success")
    }

    override fun onFailure(e: Exception?) {
        println("failed")
    }
}

fun main() {
    println("foo")
}
