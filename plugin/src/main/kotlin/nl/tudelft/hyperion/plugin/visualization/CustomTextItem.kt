package nl.tudelft.hyperion.plugin.visualization

/**
 * ComboBox item with a different string representation than the value.
 *
 * @param T the type of the internal value
 * @property v the value.
 * @property format the string format to use when displaying this value.
 */
data class CustomTextItem<T>(
    val v: T,
    val format: String
) {
    override fun toString(): String = format.format(v)
}
