package nl.tudelft.hyperion.pipeline.renamer

import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Represents a single renaming entry, renaming [from] to [to].
 */
class Rename(val from: String, val to: String) {
    /**
     * Lazily caching list of [to] delimited by '.'.
     */
    val toParts by lazy {
        to.split(".")
    }

    /**
     * Lazily computed list of [toParts] with the exception
     * of the last item.
     */
    val toPath by lazy {
        toParts.subList(0, toParts.size - 1)
    }

    /**
     * Lazily computed last item of [toPath].
     */
    val toFieldName by lazy {
        toParts.last()
    }

    /**
     * Lazily computed last item of [from].
     */
    val fromFieldName by lazy {
        from.split(".").last()
    }
}

/**
 * Configuration for renaming plugin
 * @param rename the list of renaming schemes
 */
data class Configuration(
    val rename: List<Rename>,
    val pipeline: PipelinePluginConfiguration
)
