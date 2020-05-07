package nl.tudelft.hyperion.aggregator.workers

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.aggregator.Configuration
import nl.tudelft.hyperion.aggregator.api.computeMetrics

/**
 * Starts a new worker that hosts a web server for API requests.
 */
fun startAPIWorker(configuration: Configuration) = GlobalScope.launch {
    val logger = mu.KotlinLogging.logger {}

    logger.debug { "Starting API worker..." }

    val app = Javalin.create {
        it.defaultContentType = "application/json"
    }.routes {
        path("/api/v1/metrics") {
            get { handleMetrics(configuration, it) }
        }
    }

    app.start(configuration.port)
    logger.debug { "API worker running on port ${configuration.port}." }
}

/**
 * Handles an HTTP call to GET /api/v1/metrics.
 */
fun handleMetrics(configuration: Configuration, ctx: Context) {
    val project = ctx.queryParam("project") ?: throw BadRequestResponse("Missing project query parameter")
    val file = ctx.queryParam("file") ?: throw BadRequestResponse("Missing file query parameter")
    val intervals = (ctx.queryParam("intervals") ?: throw BadRequestResponse("Missing intervals query parameter"))
        .split(",")
        .map {
            if (it.contains(Regex("[^0-9]"))) {
                throw BadRequestResponse("'intervals' query must contain numbers only")
            }

            it
        }
        .map { it.toInt() }

    ctx.json(computeMetrics(configuration, project, file, intervals))
}
