package nl.tudelft.hyperion.aggregator.workers

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.aggregator.Configuration
import nl.tudelft.hyperion.aggregator.api.computeMetrics
import nl.tudelft.hyperion.aggregator.api.computePeriodicMetrics

/**
 * Starts a new worker that hosts a web server for API requests.
 */
fun startAPIWorker(configuration: Configuration): Job {
    val logger = mu.KotlinLogging.logger {}

    logger.debug { "Starting API worker..." }

    val app = Javalin.create {
        it.defaultContentType = "application/json"
    }.routes {
        path("/api/v1/metrics") {
            get { handleMetrics(configuration, it) }
        }

        path("/api/v1/metrics/period") {
            get { handlePeriodicMetrics(configuration, it) }
        }
    }

    app.start(configuration.port)
    logger.debug { "API worker running on port ${configuration.port}." }

    return GlobalScope.launch {
        try {
            while (isActive) {
                delay(Long.MAX_VALUE)
            }
        } finally {
            logger.debug { "Stopping API worker..." }
            app.stop()
        }
    }
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

/**
 * Handles an HTTP call to GET /api/v1/metrics/period.
 */
fun handlePeriodicMetrics(configuration: Configuration, ctx: Context) {
    val project = ctx.queryParam("project") ?: throw BadRequestResponse("Missing project query parameter")
    val relativeTime = (ctx.queryParam("relative-time") ?: throw BadRequestResponse(
        "Missing relative-time query parameter"
    ))
        .also {
            if (Regex("[^0-9]").containsMatchIn(it)) {
                throw BadRequestResponse("'startTime' query parameter must be a number")
            }
        }
    val steps = (ctx.queryParam("steps") ?: throw BadRequestResponse("Missing steps query parameter"))
        .also {
            if (Regex("[^0-9]").containsMatchIn(it)) {
                throw BadRequestResponse("'steps' query parameter must be a number")
            }
        }

    // Query statistics of entire project if file is not given.
    val file = ctx.queryParam("file")

    ctx.json(computePeriodicMetrics(configuration, project, file, relativeTime.toInt(), steps.toInt()))
}
