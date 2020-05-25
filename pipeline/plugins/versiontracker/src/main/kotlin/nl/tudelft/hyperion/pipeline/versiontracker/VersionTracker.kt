package nl.tudelft.hyperion.pipeline.versiontracker

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.*
import mu.KotlinLogging
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import java.util.concurrent.ConcurrentHashMap

const val UPDATE_INTERVAL = 300_000L

class VersionTracker(private val config: Configuration) : AbstractPipelinePlugin(config.zmq) {

    private val projectVersions = ConcurrentHashMap<String, String>()
    private val mapper = jacksonObjectMapper()
    private val logger = KotlinLogging.logger {}

    init {
    }

    private fun CoroutineScope.runVersionResolver(): Job = launch {
        while (isActive) {
            withContext(Dispatchers.IO) {
            }

            delay(UPDATE_INTERVAL)
        }
    }

    private fun resolveCommitHash(input: String): String? {
        val root: JsonNode = mapper.readTree(input)

        val projectName = root.get("project").textValue() ?: return null

        // Add commit hash if the project exists
        if (projectVersions.containsKey(projectName)) {
            val objectRoot = root as ObjectNode
            objectRoot.put("commit", projectVersions.getValue(projectName))
            return objectRoot.toString()
        }

        logger.warn { "Project with $projectName is not defined as a known project" }

        return null
    }

    override suspend fun process(input: String): String? = resolveCommitHash(input)
}