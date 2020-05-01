@file:JvmName("Main")

package aggregator

fun main(vararg args: String) {
    println("HAHA aggregator goes brrrrr")
}

fun add(x: Int, y: Int): Int {
    if (x == y) {
        return 2 * x
    }
    return x + y
}
