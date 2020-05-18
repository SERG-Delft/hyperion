package nl.tudelft.hyperion.pipeline.pathextractor

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Function that replaces the value of a package field with the actual java class path
 * @param input The json string
 * @param config The path renaming configuration
 * @return A JSON string with the new value
 */
fun extractPath(input : String, config : Configuration) : String {
    val mapper = jacksonObjectMapper()
    val tree = mapper.readTree(input)

    val parent = tree.findParent(config.field)

    if (parent != null) {
        val packageName = tree.findValue(config.field).toString().drop(1).dropLast(1).split(".")

        var path = config.relativePathFromSource
        for (subfolder in packageName) {
            path += "/"
            path += subfolder
        }
        path += config.postfix

        (parent as ObjectNode).remove(config.field)
        parent.put(config.field, path)
    }

    return tree.toString()
}
