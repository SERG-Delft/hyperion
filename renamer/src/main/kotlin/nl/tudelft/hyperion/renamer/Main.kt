@file:JvmName("Main")

package nl.tudelft.hyperion.renamer

import java.nio.file.Path


fun main(vararg args: String) {
    val instance = RenamePlugin(Configuration.load(Path.of(args.get(0)).toAbsolutePath()))
    Thread.sleep(Long.MAX_VALUE)
}
