package nl.tudelft.hyperion.pipeline.extractor

import com.fasterxml.jackson.annotation.JsonProperty
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

class Extract(val to: String, val type: Type)

/**
 * Configuration for the extraction plugin
 * @param field the name of the field to be matched
 * @param match the regex pattern to match the field on
 * @param extract list of extraction schemes
 */
data class Configuration(
    val plugin: PipelinePluginConfiguration,
    val field: String,
    val match: String,
    val extract: List<Extract>
)

enum class Type {
    @JsonProperty("string")
    STRING,

    @JsonProperty("number")
    NUMBER,

    @JsonProperty("double")
    DOUBLE
}
