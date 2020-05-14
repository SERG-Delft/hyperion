package nl.tudelft.hyperion.extractor

import nl.tudelft.hyperion.pipeline.PipelineRedisConfiguration

/**
 * Data class for naming extracted information
 * @param to : The name of the new field
 * @param type : type of the extracted value
 */
data class Extract(val to : String, val type : String)

/**
 * Configuration for field extraction
 */
data class Configuration(
    val redis: PipelineRedisConfiguration,
    var registrationChannelPostfix: String?,
    val name: String,
    val field : String,
    val match : String,
    val extract: List<Extract>
)
