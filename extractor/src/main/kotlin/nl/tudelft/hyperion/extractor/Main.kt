@file:JvmName("Main")

package nl.tudelft.hyperion.extractor

import java.nio.file.Path

fun main(vararg args: String) {
    val instance = ExtractPlugin(Configuration.load(Path.of(args.get(0)).toAbsolutePath()))
    Thread.sleep(Long.MAX_VALUE)
}

