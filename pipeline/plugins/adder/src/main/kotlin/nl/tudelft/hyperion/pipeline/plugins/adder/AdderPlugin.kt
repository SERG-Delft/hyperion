package nl.tudelft.hyperion.pipeline.plugins.adder

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import nl.tudelft.hyperion.pipeline.readYAMLConfig
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey

/**
 * Pipeline plugin which adds (key, value) pairs to incoming JSON messages.
 * Returns the updated JSON message.
 *
 * @param config: [AdderConfiguration] which specifies default plugin details and which fields to add.
 */
class AdderPlugin(var config: AdderConfiguration) : AbstractPipelinePlugin(config.pipeline) {

    private val mapper = ObjectMapper()
    private val logger = mu.KotlinLogging.logger {}

    /**
     * Helper function that will get or create an object child
     * of the current object node.
     */
    private fun ObjectNode.findOrCreateChild(name: String): ObjectNode {
        if (this.get(name) != null) {
            return this.get(name) as ObjectNode
        }

        return this.putObject(name)
    }

    /**
     * Takes the input string and applies all [AddConfiguration] to it.
     * Expects a json formatted string as input, returns a json formatted string.
     * Uses the given mapper to convert the input string to a tree.
     * Returns the input when string cannot be parsed.
     */
    @Suppress("TooGenericExceptionCaught")
    override suspend fun process(input: String): String {
        // parse json string
        val tree = try {
            mapper.readTree(input) as ObjectNode
        } catch (ex: Exception) {
            logger.error(ex) { "Adder plugin [${config.pipeline.id}] was not able to parse $input" }
            return input
        }

        for (item in config.add) {
            val target = item.path.fold(tree, { p, c ->
                p.findOrCreateChild(c)
            })

            // If the property does not exist, or if it is `null` and we have overwriting null enabled...
            if (target.get(item.fieldName) == null || (target.get(item.fieldName) is NullNode && item.overwriteNull)) {
                target.put(item.fieldName, item.value)
            }
        }

        return tree.toString()
    }

    fun launchUpdateConfigFileChanged(path: String) = GlobalScope.launch {
        updateConfigFileChanged(path)
    }

    /**
     * Will watch the given config file path for changes.
     * When the file has changed it will update the configuration.
     * @param path The path to the configuration file.
     */
    private fun updateConfigFileChanged(path: String) {
        logger.info { "Launching config file listener" }
        // setup directory changed watcher for the configuration file
        val watchService = FileSystems.getDefault().newWatchService()
        val configName = Path.of(path).fileName
        val configDir = Path.of(path).parent
        configDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)

        // poll for file changes
        logger.info { "Start polling loop" }
        var key: WatchKey
        while (watchService.take().also { key = it } != null) {
            for (event in key.pollEvents()) {
                val filename = Path.of(event.context().toString())
                val fileLocation = configDir.resolve(filename)
                // only update config when config file changed in directory
                if (filename == configName) {
                    logger.info { "Configuration file has been changed" }
                    updateConfig(fileLocation.toString())
                }
            }
            key.reset()
        }
    }

    /**
     * Updates configuration file with file on given path.
     * Will only apply new AddConfiguration instances, new config won't be requested.
     * @param path The Path to the configuration file which should be loaded.
     */
    fun updateConfig(path: String) {
        val newConfig = readYAMLConfig<AdderConfiguration>(Path.of(path))
        config = newConfig
        logger.info { "Configuration has been updated" }
    }
}
