package nl.tudelft.hyperion.plugin.connection

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.project.Project
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import nl.tudelft.hyperion.plugin.metric.FileMetrics
import nl.tudelft.hyperion.plugin.settings.HyperionSettings

object APIRequestor {
    private val client = HttpClient(CIO)
    private val mapper = ObjectMapper()

    init {
        val module = KotlinModule()
        mapper.registerModule(module)
    }

    suspend fun getMetricForFile(filePath: String, ideProject: Project, httpClient: HttpClient = this.client):
            FileMetrics {
        val state = HyperionSettings.getInstance(ideProject).state
        val intervals = state.intervals.joinToString(",")
        val project = state.project

        val json: String = httpClient.get(
                "${state.address}/api/v1/metrics?project=$project&file=$filePath&intervals=$intervals"
        )

        return FileMetrics.fromMetricsResults(mapper.readValue(json))
    }
}
