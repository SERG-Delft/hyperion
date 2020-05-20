package nl.tudelft.hyperion.plugin.connection

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import nl.tudelft.hyperion.plugin.metric.MetricsResult

object ApiRequestor {
    private val client = HttpClient(CIO)
    private val mapper = ObjectMapper()

    init {
        val module = KotlinModule()
        module.addDeserializer(MetricsResult::class.java, MetricDeserializer())
        mapper.registerModule(module)
    }

    public suspend fun getMetricForFile(filePath: String): MetricsResult {
        // TODO: Remove hardcoded intervals & project
        val intervals = "60,3600,86400"
        val project = "TestProject"

        // val json: String = client.get("/api/v1/metrics?project=$project&file=$filePath&intervals=$intervals")
        val json = """
            [{
                "interval": 60,
                "versions": {
                    "abc": [{
                        "line": 10,
                        "count": 20,
                        "severity": "INFO"
                    }, {
                        "line": 20,
                        "count": 20,
                        "severity": "INFO"
                    }],
                    "def": [{
                        "line": 20,
                        "count": 1,
                        "severity": "DEBUG"
                    }]
                }
            }, {
                "interval": 120,
                "versions": {
                    "abc": [{
                        "line": 10,
                        "count": 20,
                        "severity": "INFO"
                    }],
                    "def": [{
                        "line": 20,
                        "count": 1,
                        "severity": "DEBUG"
                    }]
                }
            }]
        """.trimIndent()

        return mapper.readValue(json, object : TypeReference<MetricsResult>() {})
    }
}

