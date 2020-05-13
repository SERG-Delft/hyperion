package nl.tudelft.hyperion.pluginmanager

import java.time.LocalDateTime

class SamplePlugin(name: String): HyperionPlugin(name) {

    override fun work(message: String): String {
        if (name == "Aggregator" && message == "10000") {
            val currentDateTime = LocalDateTime.now()
            println(currentDateTime)
        }
        return message
    }
}