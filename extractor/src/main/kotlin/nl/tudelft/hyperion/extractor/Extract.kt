package nl.tudelft.hyperion.extractor

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files
import java.nio.file.Path

fun extract(path : Path, config : Configuration) : String {
    val input = Files.readString(path)

    return extract(input, config)
}

fun extract(input : String, config : Configuration) : String {
    val mapper = jacksonObjectMapper()
    val tree = mapper.readTree(input)

    val fieldValue = tree.findValue(config.field).toString()
    val pattern = Regex(config.match)
    val matches = pattern.find(fieldValue)

    val extracts = config.extract
    var i = 0
    matches?.groupValues?.forEach {
        if(i > 0) {
            val extract = extracts[i-1]
            when (extract.type) {
                "number" -> (tree.findParent(config.field) as ObjectNode).put(extract.to, it.toInt())
                "double" -> (tree.findParent(config.field) as ObjectNode).put(extract.to, it.toDouble())
                "string" -> (tree.findParent(config.field) as ObjectNode).put(extract.to, it)
                else -> (tree.findParent(config.field) as ObjectNode).put(extract.to, it)
            }
        }
        i++
    }

    return tree.toPrettyString()
}