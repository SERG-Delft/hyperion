package nl.tudelft.hyperion.pluginmanager

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.zeromq.SocketType
import org.zeromq.ZMQ

/**
 * :PluginManager: Takes in configuration and sets up a simple ZMQ REQ socket to answers register calls.
 * Will tell each registering plugin to what port it should bind or connect.
 * Will log on illegal register attempts and crash on invalid configuration file.
 */
class PluginManager(config: Configuration) {

    private val host = config.host
    private val plugins = config.plugins

    private val logger = mu.KotlinLogging.logger {}
    private val mapper = ObjectMapper()

    init {
        logger.info { "Initialized PluginManager" }
    }

    /**
     * Launches an infinite loop which handles registration requests from pipeline plugins.
     */
    fun launchListener() {
        val context = ZMQ.context(1)
        val responder = context.socket(SocketType.REP)

        responder.bind(host)
        logger.info { "Connected ZMQ reply to $host" }
        logger.info { "Launching REQ/REP loop" }
        while (!Thread.currentThread().isInterrupted) {
            //  Wait for next request from client
            val request = responder.recvStr(0)
            logger.debug { "Received: [$request]" }

            handleRegister(request, responder)
        }

        // cleanup
        responder.close()
        context.term()
    }

    /**
     * Handles an incoming register event.
     * Replies with "Invalid Request" on invalid messages.
     */
    @Suppress("TooGenericExceptionCaught")
    fun handleRegister(request: String, res: ZMQ.Socket) {
        val reqMap: MutableMap<*, *> = try {
            verifyRequest(request)
        } catch (ex: Exception) {
            logger.error { ex }
            res.send("Invalid Request")
            return
        }

        logger.info { "Received register request: [$reqMap]" }
        val plugin = reqMap["id"].toString()
        try {
            val response = when (reqMap["type"].toString()) {
                "push" -> registerPush(plugin)
                "pull" -> registerPull(plugin)
                else -> null
            }
            logger.info { "Responding with: $response" }
            res.send(response)
        } catch (ex: Exception) {
            logger.error { ex }
            res.send("Invalid Request")
            return
        }
    }

    /**
     * Verifies whether the incomming message is valid and parses it to a Map.
     * Currently accepts {"id": "pluginName", "type": "typeName"} messages
     * where pluginName should be in the configuration and typeName should be either "push" or "pull"
     */
    private fun verifyRequest(request: String): MutableMap<*, *> {
        // try to convert message to a map
        val map = mapper.readValue(
            request,
            MutableMap::class.java
        )

        if (!map.containsKey("id")) {
            throw NoSuchFieldException("Request should contain id field but did not")
        } else if (!map.containsKey("type")) {
            throw NoSuchFieldException("Request should include type field but did not")
        } else if (map["type"] != "push" && map["type"] != "pull") {
            throw IllegalArgumentException("Incorrect request type ${map["type"]} received")
        }

        // check whether the plugin that tries to register is in the config
        getPlugin(map["id"].toString()) ?: throw IllegalArgumentException(
            "Plugin ${map["id"]} does not exist in current pipeline"
        )

        return map
    }

    /**
     * Creates the return message for a plugin which registers as a push plugin.
     */
    private fun registerPush(pluginName: String): String {
        val plugin = getPlugin(pluginName)
        val isLastPlugin = plugin != null && plugins.last() == plugin

        // Return null if plugin doesn't exist or if this is the last in the pipeline.
        return PeerConnectionInformation(
            if (plugin != null && !isLastPlugin) plugin.host else null,
            true
        ).serialize()
    }

    /**
     * Creates the return message for a plugin which registers as a pull plugin.
     */
    private fun registerPull(pluginName: String): String {
        return PeerConnectionInformation(
            previousPlugin(pluginName)?.host,
            false
        ).serialize()
    }

    /**
     * @returns the plugin with the specified ID, or null if unknown
     */
    private fun getPlugin(pluginName: String): PipelinePluginConfig? {
        return plugins.find { it.id == pluginName }
    }

    /**
     * @returns the plugin before the specified plugin, or null if unknown or the first entry
     */
    private fun previousPlugin(pluginName: String): PipelinePluginConfig? {
        val plugin = getPlugin(pluginName) ?: return null
        val pluginIdx = plugins.indexOf(plugin)

        return if (pluginIdx == 0) {
            null
        } else {
            plugins.getOrNull(pluginIdx - 1)
        }
    }
}

/**
 * Represents the information needed for this plugin to connect to any
 * future elements in the pipeline. Contains the host, port and whether
 * it needs to bind or connect to that specific element.
 *
 * Host may optionally be null if the specified plugin does not
 * have an connection on the end that it requested (i.e. if the plugin
 * requests pull and host is null, it means that it is the first step
 * in the pipeline).
 */
data class PeerConnectionInformation(
    val host: String?,
    val isBind: Boolean
) {
    companion object {
        val mapper = jacksonObjectMapper()
    }

    fun serialize() = mapper.writeValueAsString(this)
}
