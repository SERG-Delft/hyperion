package nl.tudelft.hyperion.pipeline.pathextractor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import nl.tudelft.hyperion.pipeline.findParent

private val mapper = ObjectMapper()

/**
 * Function that replaces the value of a package field with the actual java class path
 * @param input The json string
 * @param config The path renaming configuration
 * @return A JSON string with the new value
 */
@Suppress("ReturnCount", "TooGenericExceptionCaught")
fun extractPath(input: String, config: Configuration): String {
    // Return value unmodified if JSON is invalid or not an object
    try {
        val tree = mapper.readTree(input) as ObjectNode
        val parent = findParent(tree, config.field)
        val packageNode = parent.findValue(config.fieldName)

        // Drop Kt suffix for kotlin support.
        val packageFields = packageNode.textValue().split(".").map {
            if (it.endsWith("Kt")) it.dropLast(2) else it
        }

        val value = "${config.relativePathFromSource}/${packageFields.joinToString("/")}${config.postfix}"
        parent.put(config.fieldName, value)

        return tree.toString()
    } catch (ex: Exception) {
        return input
    }
}
