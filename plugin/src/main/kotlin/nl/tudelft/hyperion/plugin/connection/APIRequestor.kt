package nl.tudelft.hyperion.plugin.connection

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import nl.tudelft.hyperion.plugin.metric.FileMetrics

object APIRequestor {
    private val client = HttpClient(CIO)
    private val mapper = ObjectMapper()

    init {
        val module = KotlinModule()
        mapper.registerModule(module)
    }

    public suspend fun getMetricForFile(filePath: String): FileMetrics {
        // TODO: Remove hardcoded intervals & project
        val intervals = "60,3600,86400"
        val project = "TestProject"

        // val json: String = client.get("/api/v1/metrics?project=$project&file=$filePath&intervals=$intervals")
        val json = """
            [{
                "interval": 60,
                "versions": {
                    "6b72b792fc2241a74592fcd11840577508de796d": [{
                        "line": 11,
                        "count": 20,
                        "severity": "INFO"
                    }, {
                        "line": 19,
                        "count": 20,
                        "severity": "INFO"
                    }],
                    "a8c21c597f39502f85090f6abf795e716ef336e2": [{
                        "line": 11,
                        "count": 1,
                        "severity": "DEBUG"
                    }]
                }
            }, {
                "interval": 120,
                "versions": {
                    "6b72b792fc2241a74592fcd11840577508de796d": [{
                        "line": 11,
                        "count": 20,
                        "severity": "INFO"
                    }],
                    "a8c21c597f39502f85090f6abf795e716ef336e2": [{
                        "line": 19,
                        "count": 1,
                        "severity": "DEBUG"
                    }]
                }
            }]
        """.trimIndent()

        return FileMetrics.fromMetricsResults(mapper.readValue(json))
    }
}

