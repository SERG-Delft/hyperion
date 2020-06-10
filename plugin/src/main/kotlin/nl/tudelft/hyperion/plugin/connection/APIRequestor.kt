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
import nl.tudelft.hyperion.plugin.metric.BaseAPIMetric
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

    /**
     * Executes an API call to get binned metrics from the address specified
     * in [HyperionSettings], it parses the result into [APIMetric] if filePath
     * is given and [FileAPIMetric] otherwise, which includes an additional
     * file field in the JSON response
     *
     * @param address the address to query from.
     * @param project the project name.
     * @param relativeTime the relative time from now.
     * @param steps the amount of bins.
     * @param filePath optional filepath to get metrics of.
     * @return the API response with interval and metrics per version.
     */
    suspend fun getBinnedMetrics(
        address: String,
        project: String,
        relativeTime: Int,
        steps: Int,
        filePath: String?
    ): APIBinMetricsResponse<out BaseAPIMetric> {
        var getURL = "$address/api/v1/metrics/period?project=$project&relative-time=$relativeTime&steps=$steps"

        val isFileOnly = filePath != null

        // add file query parameter if not null
        // this results in retrieving project wide statistics
        if (isFileOnly) {
            getURL += "&file=$filePath"
        }

        val json: String = client.get(getURL)

        return if (isFileOnly) {
            mapper.readValue<APIBinMetricsResponse<APIMetric>>(json)
        } else {
            mapper.readValue<APIBinMetricsResponse<FileAPIMetric>>(json)
        }
    }
}
