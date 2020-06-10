package nl.tudelft.hyperion.plugin.visualization

data class CustomTextItem<T>(
    val v: T,
    val format: String
) {
    override fun toString(): String = format.format(v)
}