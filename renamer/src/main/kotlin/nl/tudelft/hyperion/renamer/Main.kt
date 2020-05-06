@file:JvmName("Main")

package nl.tudelft.hyperion.renamer

import java.nio.file.Path


fun main(vararg args: String) {
    val config = Configuration.load(Path.of("./renamer/config.yaml").toAbsolutePath())

    val renamedJSON = renameFromPath(Path.of("./renamer/example.json").toAbsolutePath(), config)
    println(renamedJSON)
}
