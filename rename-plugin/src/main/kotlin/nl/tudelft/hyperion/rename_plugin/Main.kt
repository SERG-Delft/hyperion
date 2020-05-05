@file:JvmName("Main")

package nl.tudelft.hyperion.rename_plugin

import java.nio.file.Path


fun main(vararg args: String) {
    val config = Configuration.load(Path.of("./rename-plugin/config.yaml").toAbsolutePath())
}