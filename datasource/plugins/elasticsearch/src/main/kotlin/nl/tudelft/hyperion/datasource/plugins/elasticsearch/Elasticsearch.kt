@file:JvmName("Main")

package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import nl.tudelft.hyperion.datasource.common.DataSourcePlugin

class Elasticsearch : DataSourcePlugin {
    override fun start() {}

    override fun stop() {}

    override fun cleanup() {}
}


fun main(vararg args: String) {
    println("foo")
}
