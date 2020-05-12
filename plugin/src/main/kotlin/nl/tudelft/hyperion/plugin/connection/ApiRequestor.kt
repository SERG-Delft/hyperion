package nl.tudelft.hyperion.plugin.connection

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.plugin.metric.LineMetrics
import nl.tudelft.hyperion.plugin.metric.MetricsResult

object ApiRequestor {
    private val client = HttpClient(CIO)
    private val mapper = ObjectMapper()
    init {
        val module = KotlinModule()
        module.addDeserializer(List::class.java, MetricDeserializer())
        mapper.registerModule(module)
    }

    public suspend fun getMetricForFile(filePath: String): List<MetricsResult<LineMetrics.IntervalMetric>> {
        // TODO: Remove hardcoded intervals & project
        val intervals = "60,3600,86400"
        val project = "TestProject"

        val json: String = client.get("/api/v1/metrics?project=$project&file=$filePath&intervals=$intervals")
        return mapper.readValue(json, object : TypeReference<List<MetricsResult<LineMetrics.IntervalMetric>>>() {})
    }

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            println(getMetricForFile(""))
            println(getMetricForFile(""))
            delay(2)
        }
        println("hi")
    }
}

