package nl.tudelft.hyperion.extractor

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files
import java.nio.file.Path

fun extract(path : Path, config : Configuration) : String {
    val input = Files.readString(path)

    return extract(input, config)
}

fun ObjectNode.findOrCreateChild(name: String): ObjectNode {
    if (this.get(name) != null) {
        return this.get(name) as ObjectNode
    }

    return this.putObject(name)
}

fun ObjectNode.put(type : String, value : String, name : String) : ObjectNode {
    val parts = name.split(".")

    if (parts.size == 1) {
        when (type) {
            "number" -> this.put(parts[0], value.toInt())
            "double" -> this.put(parts[0], value.toDouble())
            else -> this.put(parts[0], value)
        }
    } else {
        val target = parts.subList(1, parts.size - 1).fold(this.findOrCreateChild(parts[0]), {
            p, c -> p.findOrCreateChild(c)
        })

        when (type) {
            "number" -> target.put(parts.last(), value.toInt())
            "double" -> target.put(parts.last(), value.toDouble())
            else -> target.put(parts.last(), value)
        }
    }

    return this
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
            (tree.findParent(config.field) as ObjectNode).put(extract.type, it, extract.to)
        }
        i++
    }

    return tree.toPrettyString()
}