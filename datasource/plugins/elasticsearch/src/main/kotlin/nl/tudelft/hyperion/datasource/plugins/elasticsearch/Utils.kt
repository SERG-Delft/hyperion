package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Object with utility functions for general purpose usage.
 */
object Utils {
    val objectMapper = ObjectMapper(JsonFactory())

    init {
        objectMapper.registerModule(KotlinModule())
    }

    /**
     * Helper function that reads a JSON object from the specified content and
     * parses it as the specified type. Will return the parsed content, or throw
     * if the content was invalid.
     */
    inline fun <reified T : Any> readJSONContent(content: String): T =
            objectMapper.readValue(content)
}
