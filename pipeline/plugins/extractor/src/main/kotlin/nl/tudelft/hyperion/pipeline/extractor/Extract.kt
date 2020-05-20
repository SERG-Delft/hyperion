package nl.tudelft.hyperion.pipeline.extractor

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files
import java.nio.file.Path

/**
 * Function that extract information from a json log message from file
 * @param path The absolute path of the json to be extracted
 * @param config A extraction configuration for the messages
 * @return A string representation of the json with additional extracted fields
 */
fun extract(path: Path, config: Configuration): String {
    val input = Files.readString(path)

    return extract(input, config)
}

/**
 * Function that checks whether a child exists and creates it otherwise
 * @param name The name of the child to be found or created
 * @return The child node as ObjectNode
 */
fun ObjectNode.findOrCreateChild(name: String): ObjectNode {
    if (this.get(name) != null) {
        return this.get(name) as ObjectNode
    }

    return this.putObject(name)
}

/**
 * Function that adds a new node with a (possibly hierarchical) path and a value to an ObjectNode
 * @param type The type of the value
 * @param value The value of the node to be added
 * @param name The location of the new node
 * @return The ObjectNode with the newly added information
 */
fun ObjectNode.put(type: Type, value: String, name: String): ObjectNode {
    val parts = name.split(".")

    if (parts.size == 1) {
        when (type) {
            Type.NUMBER -> this.put(parts[0], value.toInt())
            Type.DOUBLE -> this.put(parts[0], value.toDouble())
            Type.STRING -> this.put(parts[0], value)
        }
    } else {
        val target = parts.subList(1, parts.size - 1).fold(this.findOrCreateChild(parts[0]), {
            p, c -> p.findOrCreateChild(c)
        })

        val leafName = parts.last()
        when (type) {
            Type.NUMBER -> target.put(leafName, value.toInt())
            Type.DOUBLE -> target.put(leafName, value.toDouble())
            Type.STRING -> target.put(leafName, value)
        }
    }

    return this
}

/**
 * Function that extracts information from a JSON string according to a configuration
 * @param input The json string
 * @param config The extraction configuration
 * @return A JSON string with additional extracted information
 */
fun extract(input: String, config: Configuration): String {
    val mapper = jacksonObjectMapper()
    val tree = mapper.readTree(input)

    val fieldValue = tree.findValue(config.field).toString()
    val pattern = Regex(config.match)
    val matches = pattern.find(fieldValue)

    val extracts = config.extract
    var i = 0
    matches?.groupValues?.forEach {
        if (i > 0) {
            val extract = extracts[i - 1]
            (tree.findParent(config.field) as ObjectNode).put(extract.type, it, extract.to)
        }
        i++
    }

    return tree.toString();
}
