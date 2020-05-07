package nl.tudelft.hyperion.aggregator.workers

import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import nl.tudelft.hyperion.aggregator.Configuration
import nl.tudelft.hyperion.aggregator.api.computeMetrics
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows

class APITest {
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
        every { ctx.queryParam("project")  } returns "test"
        every { ctx.queryParam("file")  } returns "test.java"
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
    }
}
