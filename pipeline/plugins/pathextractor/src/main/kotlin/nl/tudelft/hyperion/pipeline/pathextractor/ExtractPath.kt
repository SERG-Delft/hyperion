package nl.tudelft.hyperion.pipeline.pathextractor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode

private val mapper = ObjectMapper()

/**
 * Helper function that will get or create an object child
 * of the current object node.
 */
fun ObjectNode.findOrCreateChild(name: String): ObjectNode? {
    if (this.get(name) != null) {
        return this.get(name) as? ObjectNode
    }

    return this.putObject(name)
}

/**
 * Function that replaces the value of a package field with the actual java class path
 * @param input The json string
 * @param config The path renaming configuration
 * @return A JSON string with the new value
 */
@Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown")
fun extractPath(input: String, config: Configuration): String {


    // Return value unmodified if not valid JSON or not an object
    try {
        val tree = mapper.readTree(input) as ObjectNode
        val parent = findParent(tree, config.field)
        val packageNode = parent.findValue(config.fieldName)

        if (packageNode is TextNode) {
            // Drop Kt suffix for kotlin support.
            val packageFields = packageNode.textValue().split(".").map {
                if (it.endsWith("Kt")) it.dropLast(2) else it
            }

            val newValue = "${config.relativePathFromSource}/${packageFields.joinToString("/")}${config.postfix}"

            val target = config.toPath.fold(tree as ObjectNode?, { p, c ->
                p?.findOrCreateChild(c)
            }) ?: throw Exception()

            target.put(config.fieldName, newValue)
        }

        return tree.toString()
    } catch (ex: Exception) {
        return input
    }
}

/**
 * Function that finds a path in a json tree
 * @param root the root of the tree
 * @param field the path
 * @return The found node
 */
fun findParent(root: ObjectNode, field: String): ObjectNode {
    val parts = field.split(".")

    if (parts.size > 1) {
        val path = "/" + field.split(".").dropLast(1).joinToString("/")

        if (root.at(path).has(field.split(".").last())) {
            return root.at(path) as ObjectNode
        }

        throw JsonFieldNotFound(field)
    }

    if (root.has(field)) {
        return root
    }

    throw JsonFieldNotFound(field)
}

data class JsonFieldNotFound(val field: String) : Exception()
