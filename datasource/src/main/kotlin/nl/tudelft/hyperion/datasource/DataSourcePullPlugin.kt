package nl.tudelft.hyperion.datasource

import kotlin.concurrent.fixedRateTimer

/**
 * Plugin that pulls new data periodically from the data source and transforms it into JSON format.
 */
abstract class DataSourcePullPlugin : DataSourcePlugin() {

    fun start() {
        var prev = System.currentTimeMillis()
        fixedRateTimer("requestScheduler", period = 1000) {
            val now = System.currentTimeMillis()
            println("diff=${now - prev}")
            prev = now

            TODO("Add operation")
        }
    }
}
