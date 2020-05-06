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

/**
 * Renames fields of a JSON string according to a configuration
 *
 * @param json a JSON string
 * @param config the renaming configuration
 * @return A JSON string with renamed fields
 */
fun rename(json : String, config: Configuration) : String {
    val mapper = jacksonObjectMapper()
    val tree = mapper.readTree(json)

    for(i in config.rename.indices) {
        if(tree.findParent(config.rename[i].from) != null) {
            val curVal = tree.findPath(config.rename[i].from)
            (tree.findParent(config.rename[i].from) as ObjectNode).put(config.rename[i].to, curVal)
            (tree.findParent(config.rename[i].from) as ObjectNode).remove(config.rename[i].from)
        }
    }

    return tree.toPrettyString()
}
