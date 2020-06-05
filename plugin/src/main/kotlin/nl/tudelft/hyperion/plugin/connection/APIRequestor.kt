package nl.tudelft.hyperion.plugin.connection

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.project.Project
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import nl.tudelft.hyperion.plugin.metric.APIBinMetricsResponse
import nl.tudelft.hyperion.plugin.metric.APIMetric
import nl.tudelft.hyperion.plugin.metric.FileAPIMetric
import nl.tudelft.hyperion.plugin.metric.FileMetrics
import nl.tudelft.hyperion.plugin.settings.HyperionSettings

object APIRequestor {
    private val client = HttpClient(CIO)
    private val mapper = ObjectMapper()

    init {
        val module = KotlinModule()
        mapper.registerModule(module)
    }

    suspend fun getMetricForFile(filePath: String, ideProject: Project): FileMetrics {
        val state = HyperionSettings.getInstance(ideProject).state
        val intervals = state.intervals.joinToString(",")
        val project = state.project

        val json: String = client.get("${state.address}?project=$project&file=$filePath&intervals=$intervals")

        return FileMetrics.fromMetricsResults(mapper.readValue(json))
    }

    // TODO: make relative time and steps retrievable from settings
    suspend fun getBinnedMetrics(
        filePath: String,
        ideProject: Project, 
        relativeTime: Int, 
        steps: Int
    ): APIBinMetricsResponse<APIMetric> {
        val state = HyperionSettings.getInstance(ideProject).state
        val project = state.project

        // add file query parameter
        // this results in retrieving project wide statistics
        val getURL = "${state.address}/period?project=$project&relative-time=$relativeTime&steps=$steps&file=$filePath"

        val json: String = client.get(getURL)

        return mapper.readValue(json)
    }

    suspend fun getBinnedMetrics(
        ideProject: Project,
        relativeTime: Int,
        steps: Int
    ): APIBinMetricsResponse<FileAPIMetric> {
        val state = HyperionSettings.getInstance(ideProject).state
        val project = state.project

        val getURL = "${state.address}/period?project=$project&relative-time=$relativeTime&steps=$steps"

        val json: String = client.get(getURL)

        return mapper.readValue(json)
    }
}

