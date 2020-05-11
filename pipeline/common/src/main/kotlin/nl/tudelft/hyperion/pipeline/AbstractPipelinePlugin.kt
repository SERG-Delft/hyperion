package nl.tudelft.hyperion.pipeline

/**
 * Represents an abstract pipeline plugin that receives some JSON
 * representation of a value and asynchronously transforms it into
 * some other representation. All communication is handled by this
 * class, such that implementors can focus on doing the actual
 * transformation.
 */
abstract class AbstractPipelinePlugin(
    private val config: PipelinePluginConfiguration
) {

    /**
     * Method that performs the plugin transform on the specified input.
     * This method can suspend, thus it can perform asynchronous transformations
     * before returning a result.
     *
     * If this method throws, its result is discarded. Since this will cause
     * data loss, it is recommended to only throw if no other options exist.
     * Returning null will also discard the result, but without the performance
     * cost of throwing/handling an exception.
     */
    abstract suspend fun process(input: String): String?
}
