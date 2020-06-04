package nl.tudelft.hyperion.aggregator.workers

import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.aggregator.Configuration
import nl.tudelft.hyperion.aggregator.api.computeMetrics
import nl.tudelft.hyperion.aggregator.api.computePeriodicMetrics
import nl.tudelft.hyperion.aggregator.utils.TestWithoutLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class APITest : TestWithoutLogging() {
    @TestFactory
    fun `Metrics handler should error on missing query params`() = listOf(
        "project",
        "file",
        "intervals"
    ).map {
        DynamicTest.dynamicTest("Metrics handler should verify that $it is present") {
            val config = Configuration("a", 1, 1, 1)
            val ctx = mockk<Context>(relaxed = true)

            every { ctx.queryParam(any()) } returns "10"
            every { ctx.queryParam(eq(it)) } returns null

            assertThrows<BadRequestResponse> {
                handleMetrics(config, ctx)
            }
        }
    }

    companion object {
        @SuppressWarnings("")
        @JvmStatic
        fun handlePeriodicMetricsInvalidQueries() = listOf(
            Arguments.of(
                "foo", "foo", "invalid", "1"
            ),
            Arguments.of(
                "foo", "foo", "12241", "invalid"
            ),
            Arguments.of(
                "foo", "foo", "478", null
            ),
            Arguments.of(
                "foo", "foo", null, "20"
            ),
            Arguments.of(
                "foo", "foo", "60", "aba123"
            ),
            Arguments.of(
                null, "foo", "60", "40"
            )
        )
    }

    @Test
    fun `Metrics handler should error if intervals contains non-numbers`() {
        val config = Configuration("a", 1, 1, 1)
        val ctx = mockk<Context>(relaxed = true)

        every {
            ctx.queryParam(any())
        } returns "a"

        every {
            ctx.queryParam("intervals")
        } returns "10,20,a,30"

        assertThrows<BadRequestResponse>("'intervals' query must contain numbers only") {
            handleMetrics(config, ctx)
        }
    }

    @Test
    fun `Metrics handler should delegate to computeMetrics`() {
        val config = Configuration("a", 1, 1, 1)
        val ctx = mockk<Context>(relaxed = true)

        // mock query params
        every { ctx.queryParam("project") } returns "test"
        every { ctx.queryParam("file") } returns "test.java"
        every { ctx.queryParam("intervals") } returns "10,20,30"

        // Mock computeMetrics.
        mockkStatic("nl.tudelft.hyperion.aggregator.api.MetricsKt")

        every {
            computeMetrics(any(), any(), any(), any())
        } returns listOf()

        // Run
        handleMetrics(config, ctx)

        // Check
        verify {
            computeMetrics(config, "test", "test.java", listOf(10, 20, 30))
        }

        unmockkAll()
    }

    @Test
    fun `Periodic metrics handler should delegate to computePeriodicMetrics`() {
        val config = Configuration("a", 1, 1, 1)
        val ctx = mockk<Context>(relaxed = true)

        // mock query params
        every { ctx.queryParam("project") } returns "test"
        every { ctx.queryParam("file") } returns "test.java"
        every { ctx.queryParam("relative-time") } returns "10"
        every { ctx.queryParam("steps") } returns "5"

        // Mock computeMetrics.
        mockkStatic("nl.tudelft.hyperion.aggregator.api.MetricsKt")

        every {
            computePeriodicMetrics(any(), any(), any(), any(), any())
        } returns Pair(5, listOf())

        // Run
        handlePeriodicMetrics(config, ctx)

        // Check
        verify {
            computePeriodicMetrics(config, "test", "test.java", 10, 5)
        }

        unmockkAll()
    }

    @ParameterizedTest
    @MethodSource("handlePeriodicMetricsInvalidQueries")
    fun `handlePeriodicMetrics should fail on invalid queries`(
        project: String?,
        file: String?,
        relativeTime: String?,
        steps: String?
    ) {
        val config = Configuration("a", 1, 1, 1)
        val ctx = mockk<Context>(relaxed = true)

        // mock query params
        every { ctx.queryParam("project") } returns project
        every { ctx.queryParam("file") } returns file
        every { ctx.queryParam("relative-time") } returns relativeTime
        every { ctx.queryParam("steps") } returns steps

        // Run
        assertThrows<BadRequestResponse> {
            handlePeriodicMetrics(config, ctx)
        }
    }

    @Test
    fun `Javalin HTTP handler should call handleMetrics`() {
        mockkStatic("nl.tudelft.hyperion.aggregator.workers.APIKt")

        every {
            handleMetrics(any(), any())
        } answers {
            secondArg<Context>()
                .status(200)
                .result("OK!")
        }

        // Find a random free port
        val port = ServerSocket(0).use {
            it.localPort
        }

        runBlocking {
            val server = startAPIWorker(
                Configuration("", port, 1, 1)
            )

            // make request
            val request = HttpRequest
                .newBuilder()
                .uri(URI("http://127.0.0.1:$port/api/v1/metrics"))
                .GET()
                .build()

            val response = HttpClient
                .newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .await()

            Assertions.assertEquals(response.body(), "OK!")
            Assertions.assertEquals(response.statusCode(), 200)

            server.cancelAndJoin()
        }

        verify(exactly = 1) {
            handleMetrics(any(), any())
        }

        unmockkAll()
    }

    @Test
    fun `Javalin HTTP handler should call handlePeriodicMetrics when period is specified`() {
        mockkStatic("nl.tudelft.hyperion.aggregator.workers.APIKt")

        every {
            handlePeriodicMetrics(any(), any())
        } answers {
            secondArg<Context>()
                .status(200)
                .result("OK!")
        }

        // Find a random free port
        val port = ServerSocket(0).use {
            it.localPort
        }

        runBlocking {
            val server = startAPIWorker(
                Configuration("", port, 1, 1)
            )

            // make request
            val request = HttpRequest
                .newBuilder()
                .uri(URI("http://127.0.0.1:$port/api/v1/metrics/period"))
                .GET()
                .build()

            val response = HttpClient
                .newHttpClient()
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .await()

            Assertions.assertEquals(response.body(), "OK!")
            Assertions.assertEquals(response.statusCode(), 200)

            server.cancelAndJoin()
        }

        verify(exactly = 1) {
            handlePeriodicMetrics(any(), any())
        }

        unmockkAll()
    }
}
