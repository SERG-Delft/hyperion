@file:JvmName("Main")

package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import java.nio.file.Path
import kotlin.system.exitProcess

const val DEFAULT_CONFIG_FILE = "config.yml"

val HELP_TEXT = """
    Periodically pull data from an Elasticsearch instance and send it to the
    Hyperion pipeline.
    """.trimIndent()

/**
 * Base command for running the Elasticsearch plugin with a config file.
 */
class Command : CliktCommand(help = HELP_TEXT) {
    override fun run() = Unit
}

/**
 * Command that verifies that the given config file is in the correct
 * format and that the fields are valid.
 */
class Verify : CliktCommand(help = "Verify if the config file is in the correct format") {
    private val path by argument(help = "Path to the config file to verify").default(DEFAULT_CONFIG_FILE)

    override fun run() {
        // TODO add more descriptive error messages
        try {
            Configuration.load(Path.of(path))
            echo("Format is correct")
        } catch (e: IllegalArgumentException) {
            echo("Provided argument is invalid\n${e.message}")
            exitProcess(-1)
        } catch (e: InvalidFormatException) {
            echo("File format is invalid\n${e.message}")
            exitProcess(-1)
        }
    }
}

/**
 * Command that starts the service with the given config file.
 */
class Run : CliktCommand(help = "Run with the the given config file") {
    private val path by argument(help = "Path to the config file").default(DEFAULT_CONFIG_FILE)

    override fun run() {
        var plugin: Elasticsearch? = null

        try {
            val config = Configuration.load(Path.of(path))
            plugin = Elasticsearch(config)
            plugin.start()

            while (true) {
                Thread.sleep(Long.MAX_VALUE)
            }
        } finally {
            plugin?.stop()
            plugin?.cleanup()
        }
    }
}

fun main(args: Array<String>) = Command()
        .subcommands(Verify(), Run())
        .main(args)
