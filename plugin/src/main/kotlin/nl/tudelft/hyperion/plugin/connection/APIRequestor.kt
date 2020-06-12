package nl.tudelft.hyperion.plugin.connection

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.project.Project
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import nl.tudelft.hyperion.plugin.metric.FileMetrics
import nl.tudelft.hyperion.plugin.settings.HyperionSettings
import java.net.BindException
import java.net.ConnectException

/**
 * Class the handles requests made to the API.
 */
object APIRequestor {
    private val client = HttpClient()
    private val mapper = ObjectMapper()

    init {
        val module = KotlinModule()
        mapper.registerModule(module)
    }

    /**
     * Method that executes the call to the API.
     * It requests all data needed from the Plugin's settings [HyperionSettings].
     *
     * This method will throw either [BindException] (in sandbox) or [ConnectException] (in production) which
     * should be handled by the caller.
     *
     * @param filePath the full filePath (relative to project root) of the file we request metrics for.
     * @param ideProject The Project opened in the IDE that has called this method. This is used to obtain the correct
     * instance of [HyperionSettings].
     * @param httpClient Optional argument in case you want to supply your own httpClient. This is currently only used
     * for testing.
     */
    suspend fun getMetricForFile(
        filePath: String,
        ideProject: Project,
        httpClient: HttpClient = this.client
    ): FileMetrics {
        val state = HyperionSettings.getInstance(ideProject).state
        val intervals = state.intervals.joinToString(",")
        val project = state.project

        val json: String = httpClient.get(
            "${state.address}/api/v1/metrics?project=$project&file=$filePath&intervals=$intervals"
        )

        return FileMetrics.fromMetricsResults(mapper.readValue(json))
    }
}
