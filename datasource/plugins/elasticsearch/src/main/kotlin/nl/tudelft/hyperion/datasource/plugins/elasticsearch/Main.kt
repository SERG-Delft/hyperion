@file:JvmName("Main")

package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option

val HELP_TEXT = """
    Periodically pull data from an Elasticsearch instance and send it to the
    Hyperion pipeline.
        
    Can either be run via a config file by passing `--file` or by manually 
    passing the necessary arguments for communication with Elasticsearch
    """.trimIndent()

class Command : CliktCommand(help = HELP_TEXT){
    val path by option(help = "Path to the config file to use")

    override fun run() = Unit
}

class Verify : CliktCommand(help = "Verify if the config file is in the correct format") {
    val path by argument(help = "Path to the config file to verify")

    override fun run() = Unit
}

class Args : CliktCommand(help = "Manually pass the necessary arguments and start") {

    override fun run() = Unit
}

fun main(args: Array<String>) = Command()
        .subcommands(Verify(), Args())
        .main(args)
