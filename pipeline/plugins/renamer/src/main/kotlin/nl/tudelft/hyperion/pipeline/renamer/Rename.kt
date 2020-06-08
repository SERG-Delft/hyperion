package nl.tudelft.hyperion.pipeline.renamer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import nl.tudelft.hyperion.pipeline.JsonFieldNotFoundException
import nl.tudelft.hyperion.pipeline.findOrCreateChild
import nl.tudelft.hyperion.pipeline.findParent
import java.lang.ClassCastException

private val mapper = ObjectMapper()

/**
 * Renames fields of a JSON string according to a configuration
 *
 * @param json a JSON string
 * @param config the renaming configuration
 * @return A JSON string with renamed fields
 */
@Suppress("LoopWithTooManyJumpStatements")
fun rename(json: String, config: Configuration): String {
    val tree = try {
        mapper.readTree(json) as ObjectNode
    } catch (ex: ClassCastException) {
        return json
    }

    for (rename in config.rename) {
        try {
            val parent = findParent(tree, rename.from)
            val value = parent.findValue(rename.fromFieldName)

            val target = rename.toPath.fold(tree as ObjectNode?, { p, c ->
                p?.findOrCreateChild(c)
            }) ?: continue

            target.put(rename.toFieldName, value)
            parent.remove(rename.fromFieldName)
        } catch (ex: JsonFieldNotFoundException) {
            continue
        }
    }

    return tree.toString()
}
