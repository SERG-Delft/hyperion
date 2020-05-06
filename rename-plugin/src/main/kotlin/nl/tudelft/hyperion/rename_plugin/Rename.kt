package nl.tudelft.hyperion.rename_plugin

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
    val curVal = tree.findPath(config.rename[0].from)

    (tree.findParent(config.rename[0].from) as ObjectNode).put(config.rename[0].to, curVal)
    (tree.findParent(config.rename[0].from) as ObjectNode).remove(config.rename[0].from)

    return tree.toPrettyString()
}