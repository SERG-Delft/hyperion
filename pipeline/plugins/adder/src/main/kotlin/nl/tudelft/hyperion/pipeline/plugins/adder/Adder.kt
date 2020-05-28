package nl.tudelft.hyperion.pipeline.plugins.adder

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

fun adder(input: String, config: List<AddConfiguration>, mapper: ObjectMapper): String {
    // parse json string
    val tree = mapper.readTree(input) as ObjectNode

    for (item in config) {
        val parts = item.key.split(".")
        val target = parts.subList(0, parts.size - 1).fold(tree, { p, c ->
            p.findOrCreateChild(c)
        })

        if (target.get(parts.last()) == null) {
            target.put(parts.last(), item.value)
        }
    }

    return tree.toString()
}
