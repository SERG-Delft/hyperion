package nl.tudelft.hyperion.pipeline

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path

/**
 * Helper function that acts as an infinite main function that runs the
 * specified pipeline plugin.
 */
@SuppressWarnings("TooGenericExceptionCaught")
inline fun <reified T : AbstractPipelinePlugin, reified C : Any> runPipelinePlugin(
    configPath: String,
    crossinline createInstance: (C) -> T
): Unit = runBlocking {
    try {
        val config = readYAMLConfig<C>(Path.of(configPath))
        val plugin = createInstance(config)

        plugin.queryConnectionInformation()
        joinAll(plugin.run())
    } catch (ex: Exception) {
        println("Failed to start ${T::class.simpleName}: ${ex.message}")
        ex.printStackTrace()
    }

    Unit
}

/**
 * Helper function that reads a YAML config from the specified path and
 * parses it as the specified configuration type. Will return the function,
 * or throw an exception if it was invalid.
 */
inline fun <reified T : Any> readYAMLConfig(path: Path): T {
    val content = Files.readString(path)
    val mapper = ObjectMapper(YAMLFactory())
    mapper.registerModule(KotlinModule())

    return mapper.readValue(content)
}

/**
 * Helper function that reads a JSON object from the specified content and
 * parses it as the specified type. Will return the parsed content, or throw
 * if the content was invalid.
 */
inline fun <reified T : Any> readJSONContent(content: String): T {
    val mapper = ObjectMapper(JsonFactory())
    mapper.registerModule(KotlinModule())

    return mapper.readValue(content)
}

/**
 * Function that finds a path in a json tree
 * @param root the root of the tree
 * @param field the path
 * @return The found node
 */
fun findParent(root: ObjectNode, field: String): ObjectNode {
    val parts = field.split(".")

    if (parts.size > 1) {
        val path = "/" + field.split(".").dropLast(1).joinToString("/")

        if (root.at(path).has(field.split(".").last())) {
            return root.at(path) as ObjectNode
        }

        throw JsonFieldNotFoundException(field)
    }

    if (root.has(field)) {
        return root
    }

    throw JsonFieldNotFoundException(field)
}

data class JsonFieldNotFoundException(val field: String) : Exception()
