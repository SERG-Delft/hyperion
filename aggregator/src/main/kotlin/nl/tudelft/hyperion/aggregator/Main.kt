@file:JvmName("Main")

package nl.tudelft.hyperion.aggregator

fun main(vararg args: String) {
    println("HAHA aggregator goes brrrrr")
}

fun add(x: Int, y: Int): Int {
    if (x == y) {
        return 2 * x
    }
    return y + x
}
