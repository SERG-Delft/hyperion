package nl.tudelft.hyperion.pipeline.plugins.adder

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import com.fasterxml.jackson.databind.node.ObjectNode
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin

/**
 * Pipeline plugin which adds (key, value) pairs to incoming JSON messages.
 * Returns the updated JSON message.
 *
 * @param config: [AdderConfiguration] which specifies default plugin details and which fields to add.
 */
class AdderPlugin(private var config: AdderConfiguration) : AbstractPipelinePlugin(config.pipeline) {

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
}
