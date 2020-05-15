@file:JvmName("Main")

package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.system.exitProcess

const val DEFAULT_CONFIG_FILE = "datasource-es.yml"

val HELP_TEXT = """
    Periodically pull data from an Elasticsearch instance and send it to the
    Hyperion pipeline.
    """.trimIndent()

/**
 * Base command for running the Elasticsearch plugin with a config file.
 */
@lombok.Generated
class Command : CliktCommand(help = HELP_TEXT) {
    override fun run() = Unit
}

/**
 * Command that verifies that the given config file is in the correct
 * format and that the fields are valid.
 */
@lombok.Generated
class Verify : CliktCommand(help = "Verify if the config file is in the correct format") {
    private val path by argument(help = "Path to the config file to verify").default(DEFAULT_CONFIG_FILE)

    override fun run() {
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
@lombok.Generated
class Run : CliktCommand(help = "Run with the the given config file") {
    private val path by argument(help = "Path to the config file").default(DEFAULT_CONFIG_FILE)

    override fun run() {
        runBlocking {
            val config = Configuration.load(Path.of(path))
            val plugin = Elasticsearch.build(config)

            plugin.queryConnectionInformation()
            val job = plugin.start()

            // add shutdown hook to cleanup
            Runtime.getRuntime().addShutdownHook(Thread {
                runBlocking {
                    job.cancelAndJoin()
                }
            })

            job.join()
        }
    }
}

@lombok.Generated
fun main(args: Array<String>) = Command()
        .subcommands(Verify(), Run())
        .main(args)
