package nl.tudelft.hyperion.pipeline.plugins.adder

import com.fasterxml.jackson.annotation.JsonProperty
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Specify which json key, value pairs should be added to each message.
 * Key will be added to top level object by default.
 * Key can be added to children by specifying each key with dots in between.
 * ```
 * {"code":{"version":"1"}}
 * ```
 * To add the field "project-name" to the code object use `code.project-name` as key.
 * Empty object will be created when no object is present.
 * The value will be the value corresponding to the key and can be any string.
 * Will not overwrite value when a key is set except for null when overwrite-null is set to true
 *
 * @param key: key which should be added
 * @param value: value which should be allocated to the key
 * @param overwriteNull: Will overwrite null values of existing key, defaults to true
 */
data class AddConfiguration(
    val key: String,
    val value: String,
    @JsonProperty("overwrite-null")
    val overwriteNull:Boolean = true
)

/**
 * Configuration for the adder pipeline plugin.
 * @param pipeline: General configuration for a pipeline plugin
 * @param add: list of [AddConfiguration] which will be applied to every incoming message
 */
data class AdderConfiguration (
    val pipeline: PipelinePluginConfiguration,
    val add: List<AddConfiguration>
)
