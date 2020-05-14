package nl.tudelft.hyperion.pluginmanager

import com.fasterxml.jackson.databind.ObjectMapper
import org.zeromq.SocketType
import org.zeromq.ZMQ

@Suppress("TooGenericExceptionCaught")
class PluginManager(config: Configuration) {

    private val host = config.host
    private val plugins = config.plugins

    private val logger = mu.KotlinLogging.logger {}

    init {
        logger.info {"Started PluginManager"}
        logger.info {"Launching REQ/REP loop"}
        launchListener()
    }

    private fun launchListener() {
        val context = ZMQ.context(1)
        val responder = context.socket(SocketType.REP)

        responder.connect(host)
        logger.info("Connected ZMQ reply to $host")

        while (!Thread.currentThread().isInterrupted) {
            //  Wait for next request from client
            val request = responder.recv(0)
            val string = String(request)

            logger.info("Received request: [$string]")
            handleRegister(string, responder)

            //  Send reply back to client
            responder.send("World".toByteArray(), 0)
        }

        // cleanup
        responder.close()
        context.term()
    }

    private fun handleRegister(request: String, res: ZMQ.Socket) {
        val mapper = ObjectMapper()
        val map = mapper.readValue(
            request,
            MutableMap::class.java
        )

        val plugin = map["id"].toString()
        when (val type = map["type"].toString()) {
            "push" -> res.send(registerPush(plugin).toByteArray(), 0)
            "pull" -> res.send(registerPull(plugin).toByteArray(), 0)
            else -> {
                res.send("Invalid Request".toByteArray())
                logger.error("Received request from $plugin with invalid type $type")
            }
        }
    }

    private fun registerPush(pluginName: String): String {
        return "\"isBind\":\"true\",\"host\":\"${nextPlugin(pluginName)["host"]!!}\""
    }

    private fun registerPull(pluginName: String): String {
        return "\"isBind\":\"false\",\"host\":\"${previousPlugin(pluginName)["host"]!!}\""
    }

    private fun previousPlugin(pluginName: String): Map<String, String> {
        val it = plugins.iterator()
        for ((index, plugin) in it.withIndex()) {
            if (plugin["name"] == pluginName) {
                return plugins[index - 1]
            }
        }
        throw IllegalArgumentException("Plugin $pluginName does not exist in current pipeline")
    }

    private fun nextPlugin(pluginName: String): Map<String, String> {
        val it = plugins.iterator()
        for ((index, plugin) in it.withIndex()) {
            if (plugin["name"] == pluginName) {
                return plugins[index + 1]
            }
        }
        throw IllegalArgumentException("Plugin $pluginName does not exist in current pipeline")
    }

}
