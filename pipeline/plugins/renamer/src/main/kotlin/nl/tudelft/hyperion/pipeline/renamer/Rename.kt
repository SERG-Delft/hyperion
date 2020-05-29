package nl.tudelft.hyperion.pipeline.renamer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * Helper function that will get or create an object child
 * of the current object node.
 */
fun ObjectNode.findOrCreateChild(name: String): ObjectNode {
    if (this.get(name) != null) {
        return this.get(name) as ObjectNode
    }

    return this.putObject(name)
}

/**
 * Renames fields of a JSON string according to a configuration
 *
 * @param json a JSON string
 * @param config the renaming configuration
 * @return A JSON string with renamed fields
 */
fun rename(json: String, config: Configuration): String {
    val mapper = ObjectMapper()
    val tree = mapper.readTree(json) as ObjectNode

    for (i in config.rename.indices) {
        val parent = tree.findParent(config.rename[i].from)
        val fieldName = config.rename[i].from.split(".").last()

        if (parent != null) {
            val value = tree.findPath(config.rename[i].from)
            val parts = config.rename[i].to.split(".")

            val target = parts.subList(0, parts.size - 1).fold(tree, { p, c ->
                p.findOrCreateChild(c)
            })

            target.put(parts.last(), value)
            parent.remove(fieldName)
        }
    }

    return tree.toString()
}
