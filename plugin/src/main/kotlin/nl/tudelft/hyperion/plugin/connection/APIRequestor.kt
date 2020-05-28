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

    public suspend fun getMetricForFile(filePath: String, ideProject: Project): FileMetrics {
        // TODO: Remove hardcoded intervals & project
        val state = HyperionSettings.getInstance(ideProject).state
        val intervals = state.intervals.joinToString(",")
        val project = "TestProject"

        val json: String = client.get("${state.address}?project=$project&file=$filePath&intervals=$intervals")

        return FileMetrics.fromMetricsResults(mapper.readValue(json))
    }
}

