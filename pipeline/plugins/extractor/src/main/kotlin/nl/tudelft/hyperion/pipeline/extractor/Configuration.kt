package nl.tudelft.hyperion.pipeline.extractor

import com.fasterxml.jackson.annotation.JsonProperty
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Configuration for the extraction plugin
 * @param pipeline the abstracted part of the configuration for a pipeline plugin
 * @param fields the list of configurations for fields to be extracted
 */
data class Configuration(
    val pipeline: PipelinePluginConfiguration,
    val fields: List<ExtractableFieldConfiguration>
)

/**
 * Configuration for a field to be extracted
 * @param field the name of the field to extract from
 * @param match the regex pattern to match the field on
 * @param extract the list of schemes for the addition of extracted information
 */
data class ExtractableFieldConfiguration(
    val field: String,
    val match: String,
    val extract: List<Extract>
) {
    val regex: Regex by lazy {
        Regex(match)
    }
}

/**
 * Data class for a naming and typing scheme
 */
data class Extract(val to: String, val type: Type)

enum class Type {
    @JsonProperty("string")
    STRING,

    @JsonProperty("number")
    NUMBER,

    @JsonProperty("double")
    DOUBLE
}
