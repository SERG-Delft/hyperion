package nl.tudelft.hyperion.aggregator.workers

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.aggregator.api.LogEntry
import nl.tudelft.hyperion.aggregator.api.LogLocation
import org.joda.time.DateTime
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun <T> CompletableFuture<T>.await(): T =
    suspendCoroutine { cont: Continuation<T> ->
        whenComplete { result, exception ->
            if (exception == null) { // the future has been completed normally
                cont.resume(result)
            } else { // the future has completed with an exception
                cont.resumeWithException(exception)
            }
        }
    }

/**
 * Starts a new temporary intake worker that will repeatedly query an ElasticSearch
 * instance with a specific index pattern for new logs. Temporary until intake is
 * handled through redis.
 */
fun startElasticSearchIntakeWorker(aggregationManager: AggregationManager) = GlobalScope.launch {
    val logger = mu.KotlinLogging.logger {}
    val client = HttpClient.newHttpClient()
    var endTime = DateTime.now()

    val mapper = ObjectMapper(JsonFactory())
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.registerModule(KotlinModule())
    mapper.registerModule(JodaModule())

    logger.debug { "Starting ES intake worker..." }

    while (isActive) {
        logger.debug { "Polling new logs..." }

        val startTime = DateTime.now()

        val req = HttpRequest.newBuilder()
            .uri(URI("http://localhost:9200/logs/_search"))
            .headers("Content-Type", "application/json")
            .POST(
                HttpRequest.BodyPublishers.ofString(
                    """
                        {
                            "_source": ["@timestamp", "log4j_line", "log4j_level", "log4j_file"],
                            "query": {
                                "bool": {
                                    "must": [
                                        {
                                            "range": {
                                                "@timestamp": {
                                                    "gte": "$endTime",
                                                    "lte": "$startTime"
                                                }
                                            }
                                        }
                                    ]
                                }
                            }
                        }
                    """.trimIndent()
                )
            )
            .build()

        val resp = client.sendAsync(req, HttpResponse.BodyHandlers.ofString()).await()
        val response = mapper.readValue(resp.body(), ESResponse::class.java)

        for (log in response.hits.hits) {
            aggregationManager.aggregate(
                LogEntry(
                    "TestProject",
                    "v1.0.0",
                    log._source.severity,
                    LogLocation(log._source.file, log._source.line),
                    log._source.timestamp
                )
            )
        }

        logger.debug { "Polled logs. Received ${response.hits.hits.count()} new log lines." }

        endTime = startTime

        // Wait for 5 seconds.
        delay(5000L)
    }

    // Never returns.
}

/* ktlint-disable ConstructorParameterNaming */
data class ESResponse(val hits: ESResponseHits)
data class ESResponseHits(val hits: List<ESHit>)
data class ESHit(val _source: ESHitSource)
data class ESHitSource(
    @JsonProperty("log4j_file")
    val file: String,
    @JsonProperty("log4j_level")
    val severity: String,
    @JsonProperty("log4j_line")
    val line: Int,
    @JsonProperty("@timestamp")
    val timestamp: DateTime
)
