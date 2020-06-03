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
        val parent = findParent(tree, rename.from)

        if (parent != null) {
            val value = findValue(tree, rename.from)

            val target = rename.toPath.fold(tree as ObjectNode?, { p, c ->
                p?.findOrCreateChild(c)
            }) ?: continue

            target.put(rename.toFieldName, value)
            parent.remove(rename.fromFieldName)
        }
    }

    return tree.toString()
}

fun findParent(root: ObjectNode, field: String): ObjectNode {
    val path = "/" + field.split(".").dropLast(1).joinToString("/")

    return root.at(path) as ObjectNode
}

fun findValue(root: ObjectNode, field: String): Int {
    val path = "/" + field.split(".").joinToString("/")

    return root.at(path).asInt()
}