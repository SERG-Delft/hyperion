@file:JvmName("Main")

package nl.tudelft.hyperion.extractor

import java.nio.file.Path

fun main(vararg args: String) {
    val config = Configuration.load(Path.of("./extractor/config.yaml").toAbsolutePath())
}
