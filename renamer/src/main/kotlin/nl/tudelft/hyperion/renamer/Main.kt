@file:JvmName("Main")

package nl.tudelft.hyperion.renamer

import java.nio.file.Path


fun main(vararg args: String) {
    val instance = RenamePlugin(Configuration.load(Path.of("./renamer/config.yaml").toAbsolutePath()).pluginConfig)
}
