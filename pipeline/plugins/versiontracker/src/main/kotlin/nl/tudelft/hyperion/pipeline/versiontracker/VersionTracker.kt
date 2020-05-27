package nl.tudelft.hyperion.pipeline.versiontracker

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import org.eclipse.jgit.lib.Ref
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Represents a plugin that track the version of a project running on a git repository
 * and adds a version field to the input based on the project the log originates from.
 * The plugin runs coroutines in the background that periodically poll some remote
 * repository to resolve the current version running on a specified branch.
 *
 * @constructor
 * Starts the poll coroutines.
 *
 * @param config the configuration to run the plugin
 */
class VersionTracker(config: Configuration) : AbstractPipelinePlugin(config.zmq) {

    val projectVersions = ConcurrentHashMap<String, String>()
    private val mapper = jacksonObjectMapper()
    private val logger = KotlinLogging.logger {}
    private val pollThreadPool = CoroutineScope(Executors.newCachedThreadPool().asCoroutineDispatcher())

    companion object {
        // what to name the added field name
        const val NEW_FIELD_NAME = "version"
    }

    init {
        for ((name, projectConfig) in config.projects) {
            logger.info { "Starting poller for $name with $projectConfig" }
            pollThreadPool.runVersionResolver(name, projectConfig)
        }
    }

    /**
     * Creates a job within the receiver's [CoroutineScope] that fetches remote
     * references every [ProjectConfig.updateInterval] from the given repository.
     *
     * @param projectName name of the project
     * @param projectConfig the necessary config for communication with git
     *  and tracking versions
     * @return the created [Job]
     */
    @SuppressWarnings("MagicNumber")
    private fun CoroutineScope.runVersionResolver(projectName: String, projectConfig: ProjectConfig): Job = launch {
        if (projectConfig.authentication is Authentication.HTTPS) {
            logger.warn {
                "Project '$projectName' is set up to fetch using username and " +
                    "password authentication, this should not be run in production"
            }
        }

        while (isActive) {
            withContext(Dispatchers.IO) {
                logger.debug { "Fetching refs for $projectName" }
                updateRefs(projectName, projectConfig)
            }

            delay(projectConfig.updateInterval * 1000L)
        }
    }

    /**
     * Retrieves all references from the remote project and checks if the
     * branch exists, update the project version with that branch's version
     * hash if it exists.
     *
     * @param projectName name of the project
     * @param projectConfig the necessary config for communication with git
     *  and tracking versions
     */
    fun updateRefs(projectName: String, projectConfig: ProjectConfig) {
        val refMap: Map<String, Ref> = when (val auth = projectConfig.authentication) {
            null ->
                lsRemoteCommandBuilder(
                    projectConfig.repository
                )

            is Authentication.SSH ->
                lsRemoteCommandBuilder(
                    projectConfig.repository,
                    auth.keyPath
                )

            is Authentication.HTTPS ->
                lsRemoteCommandBuilder(
                    projectConfig.repository,
                    auth.username,
                    auth.password
                )
        }.callAsMap()

        // check if the the branch name is in the references
        if (refMap.containsKey(projectConfig.branch) && refMap[projectConfig.branch] != null) {
            logger.debug { "Updating entry for $projectName with ${refMap[projectConfig.branch]}" }

            projectVersions[projectName] =
                (refMap[projectConfig.branch]
                    ?: error("Reference with branch ${projectConfig.branch} is null"))
                    .objectId.name
        }
    }

    /**
     * Extracts the "project" field from the given JSON input string,
     * subsequently checks if the project with that value exists in the
     * stored map of projects and versions. The version is then added
     * to the field [NEW_FIELD_NAME] if the project exists in the map.
     *
     * @param input input message from the Hyperion pipeline
     * @return JSON string with the added field,
     *  will return null if the project does not exist
     */
    @SuppressWarnings("ReturnCount")
    fun resolveCommitHash(input: String): String? {
        val root: JsonNode = mapper.readTree(input)

        val projectNode = root.get("project") ?: return null
        val projectName = projectNode.textValue()

        // Add commit hash if the project exists
        if (projectVersions.containsKey(projectName)) {
            val objectRoot = root as ObjectNode
            objectRoot.put(NEW_FIELD_NAME, projectVersions.getValue(projectName))
            return objectRoot.toString()
        }

        logger.warn { "Project with $projectName is not defined as a known project" }

        return null
    }

    override suspend fun process(input: String): String? = resolveCommitHash(input)
}
