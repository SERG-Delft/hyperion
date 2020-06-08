package nl.tudelft.hyperion.pipeline.extractor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import nl.tudelft.hyperion.pipeline.JsonFieldNotFoundException
import nl.tudelft.hyperion.pipeline.findParent

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
        try {
            when (type) {
                Type.NUMBER -> this.put(parts[0], value.toInt())
                Type.DOUBLE -> this.put(parts[0], value.toDouble())
                Type.STRING -> this.put(parts[0], value)
            }
        } catch (ex: NumberFormatException) {
            this.put(parts[0], value)
            ex.printStackTrace()
        }
    } else {
        val target = parts.subList(1, parts.size - 1).fold(this.findOrCreateChild(parts[0]), { p, c ->
            p.findOrCreateChild(c)
        })

        val leafName = parts.last()

        try {
            when (type) {
                Type.NUMBER -> target.put(leafName, value.toInt())
                Type.DOUBLE -> target.put(leafName, value.toDouble())
                Type.STRING -> target.put(leafName, value)
            }
        } catch (ex: NumberFormatException) {
            target.put(leafName, value)
            ex.printStackTrace()
        }
    }

    return this
}

private val mapper = ObjectMapper()

/**
 * Function that extracts information from a JSON string according to a configuration
 * @param input The json string
 * @param config The extraction configuration
 * @return A JSON string with additional extracted information
 */
@Suppress("TooGenericExceptionCaught")
fun extract(input: String, config: Configuration): String {
    val tree = try {
        mapper.readTree(input) as ObjectNode
    } catch (ex: Exception) {
        return input
    }

    for (extractableField in config.fields) {
        try {
            val parent = findParent(tree, extractableField.field)
            val value = parent.findValue(extractableField.fieldName).toString()
            val pattern = extractableField.regex
            val matches = pattern.find(value)

            matches?.groupValues?.drop(1)?.zip(extractableField.extract)?.forEach { (match, extract) ->
                tree.put(extract.type, match, extract.to)
            }
        } catch (ex: JsonFieldNotFoundException) {
            continue
        }
    }

    return tree.toString()
}
