package nl.tudelft.hyperion.renamer

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files
import java.nio.file.Path

/**
 * Renames fields of JSON content retrieved from a file
 *
 * @param jsonPath the path to the JSON file
 * @param config the renaming configuration
 * @return A JSON string with renamed fields
 */
fun renameFromPath(jsonPath : Path, config : Configuration) : String {
    val json = Files.readString(jsonPath)

    return rename(json, config)
}

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
fun rename(json : String, config: Configuration) : String {
    val mapper = jacksonObjectMapper()
    val tree = mapper.readTree(json) as ObjectNode

    for (i in config.rename.indices) {
        val parent = tree.findParent(config.rename[i].from)
        if (parent != null) {
            val value = tree.findPath(config.rename[i].from)
            val parts = config.rename[i].to.split(".")

            if (parts.size == 1) {
                tree.put(parts[0], value)
            } else {
                val target = parts.subList(1, parts.size - 1).fold(tree.findOrCreateChild(parts[0]), {
                    p, c -> p.findOrCreateChild(c)
                })

                target.put(parts.last(), value)
            }
        }
    }

    return tree.toString()
}
