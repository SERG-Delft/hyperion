package nl.tudelft.hyperion.pathextractor

import nl.tudelft.hyperion.pipeline.PipelineRedisConfiguration

/**
 * Configuration for field extraction
 */
data class Configuration(
        val redis: PipelineRedisConfiguration,
        var registrationChannelPostfix: String?,
        val name: String,
        val field : String
)
