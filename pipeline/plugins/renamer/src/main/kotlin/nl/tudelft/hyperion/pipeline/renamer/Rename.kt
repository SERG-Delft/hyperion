package nl.tudelft.hyperion.pipeline.renamer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

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

private val mapper = ObjectMapper()

/**
 * Renames fields of a JSON string according to a configuration
 *
 * @param json a JSON string
 * @param config the renaming configuration
 * @return A JSON string with renamed fields
 */
@Suppress("TooGenericExceptionCaught")
fun rename(json: String, config: Configuration): String {
    val tree = try {
        mapper.readTree(json) as ObjectNode
    } catch (ex: Exception) {
        return json
    }

    for (rename in config.rename) {
        try {
            val parent = findParent(tree, rename.from)
            val value = parent.findValue(rename.fromFieldName)

            val target = rename.toPath.fold(tree as ObjectNode?, { p, c ->
                p?.findOrCreateChild(c)
            }) ?: continue

            target.put(rename.toFieldName, value)
            parent.remove(rename.fromFieldName)
        } catch (ex: Exception) {
            continue
        }
    }

    return tree.toString()
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

        return root.at(path) as ObjectNode
    } else {
        if (root.has(field)) {
            return root
        } else {
            throw Exception()
        }
    }
}