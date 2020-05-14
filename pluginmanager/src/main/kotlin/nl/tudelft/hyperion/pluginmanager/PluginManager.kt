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
        logger.info {"Initialized PluginManager"}
    }

    fun launchListener() {
        val context = ZMQ.context(1)
        val responder = context.socket(SocketType.REP)

        responder.connect(host)
        logger.info("Connected ZMQ reply to $host")
        logger.info {"Launching REQ/REP loop"}
        while (!Thread.currentThread().isInterrupted) {
            //  Wait for next request from client
            val request = responder.recvStr(0)

            logger.info("Received request: [$request]")
            handleRegister(request, responder)
        }

        // cleanup
        responder.close()
        context.term()
    }

    fun handleRegister(request: String, res: ZMQ.Socket) {
        val mapper = ObjectMapper()
        val map = mapper.readValue(
            request,
            MutableMap::class.java
        )

        val plugin = map["id"].toString()
        when (val type = map["type"].toString()) {
            "push" -> res.send(registerPush(plugin))
            "pull" -> res.send(registerPull(plugin))
            else -> {
                res.send("Invalid Request")
                logger.error("Received request from $plugin with invalid type $type")
            }
        }
    }

    private fun registerPush(pluginName: String): String {
        return "{\"isBind\":\"true\",\"host\":\"${getPlugin(pluginName)["host"]!!}\"}"
    }

    private fun registerPull(pluginName: String): String {
        return "{\"isBind\":\"false\",\"host\":\"${previousPlugin(pluginName)["host"]!!}\"}"
    }

    private fun getPlugin(pluginName:String): Map<String, String> {
        val it = plugins.iterator()
        for (plugin in it) {
            if (plugin["name"] == pluginName) {
                return plugin
            }
        }
        throw IllegalArgumentException("Plugin $pluginName does not exist in current pipeline")
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

}
