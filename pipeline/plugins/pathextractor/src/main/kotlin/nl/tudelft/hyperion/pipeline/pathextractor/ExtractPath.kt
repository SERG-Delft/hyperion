package nl.tudelft.hyperion.pipeline.pathextractor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode

private val mapper = ObjectMapper()

/**
 * Function that replaces the value of a package field with the actual java class path
 * @param input The json string
 * @param config The path renaming configuration
 * @return A JSON string with the new value
 */
@Suppress("TooGenericExceptionCaught")
fun extractPath(input: String, config: Configuration): String {
    val tree = mapper.readTree(input)

    // Return value unmodified if not valid JSON or not an object
    val parent = try {
        tree.findParent(config.field) as ObjectNode
    } catch (ex: Exception) {
        return input
    }

    val packageNode = tree.findValue(config.field)

    if (packageNode is TextNode) {
        // Drop Kt suffix for kotlin support.
        val packageFields = packageNode.textValue().split(".").map {
            if (it.endsWith("Kt")) it.dropLast(2) else it
        }

        val newValue = "${config.relativePathFromSource}/${packageFields.joinToString("/")}${config.postfix}"
        parent.put(config.field, newValue)
    }

    return tree.toString()
}
