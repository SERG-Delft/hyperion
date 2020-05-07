package nl.tudelft.hyperion.extractor

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun Extract(input : String, config : Configuration) : String {
    val mapper = jacksonObjectMapper()
    val tree = mapper.readTree(input)

    val field = tree.findValue(config.field).toString()
    val pattern = Regex(config.match)
    val match = pattern.find(field)

    (tree.findParent(config.field) as ObjectNode).put(config.rename.to, match?.value)

    return tree.toPrettyString()
}