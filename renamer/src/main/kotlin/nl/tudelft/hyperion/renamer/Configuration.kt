package nl.tudelft.hyperion.renamer

import nl.tudelft.hyperion.pipeline.PipelineRedisConfiguration

class Rename(val from: String, val to: String)

/**
 * Configuration for renaming plugin
 * @param rename the list of renaming schemes
 */
data class Configuration(
    val rename: List<Rename>,
    val redis: PipelineRedisConfiguration,
    var registrationChannelPostfix: String?,
    val name: String
)
