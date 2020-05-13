package nl.tudelft.hyperion.aggregator.intake

import io.lettuce.core.ConnectionFuture
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisFuture
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.aggregator.RedisConfiguration
import nl.tudelft.hyperion.aggregator.utils.TestWithoutLogging
import nl.tudelft.hyperion.aggregator.workers.AggregationManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.RuntimeException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class CompletedRedisFuture<T>(val value: T?) : CompletableFuture<T>(), RedisFuture<T> {
    init {
        this.complete(value)
    }

    override fun getError() = null
    override fun await(timeout: Long, unit: TimeUnit?) = true
}

class RedisIntakeTest : TestWithoutLogging() {
    // Note: ideally I would split this into testing both setup and
    // setupPubSubListener individually, but for some reason mockk
    // does not like a spy stubbing out the setupPubSubListener
    // call. Oh well. Guess we need to test them both at the same time.
    @Test
    fun `Setup should query the subChannel to subscribe on, then subscribe to that channel`() {
        mockkStatic("io.lettuce.core.RedisClient")

        val client = mockk<RedisClient>(relaxed = true)
        val connection = mockk<StatefulRedisConnection<String, String>>(relaxed = true)
        val pubsubConnection = mockk<StatefulRedisPubSubConnection<String, String>>(relaxed = true)

        every {
            RedisClient.create(any<RedisURI>())
        } returns client

        every {
            client.connectAsync(StringCodec.UTF8, any())
        } returns ConnectionFuture.completed(null, connection)

        every {
            client.connectPubSubAsync(StringCodec.UTF8, any())
        } returns ConnectionFuture.completed(null, pubsubConnection)

        every {
            connection.async().hget(any(), any())
        } returns CompletedRedisFuture("channel")

        every {
            pubsubConnection.async().subscribe(any())
        } returns CompletedRedisFuture<Void>(null)

        val intake = spyk(
            RedisIntake(
                RedisConfiguration("localhost"),
                mockk()
            )
        )

        runBlocking {
            intake.setup()
        }

        coVerify(exactly = 1) {
            client.connectAsync(StringCodec.UTF8, any())

            connection.async().hget(any(), "subChannel")

            intake.setupPubSubListener("channel")

            client.connectPubSubAsync(StringCodec.UTF8, any())

            pubsubConnection.addListener(intake)

            pubsubConnection.async().subscribe("channel")
        }

        unmockkAll()
    }

    @Test
    fun `Setup should gracefully handle connection errors`() {
        mockkStatic("io.lettuce.core.RedisClient")
        
        every {
            RedisClient.create(any<RedisURI>()).connectAsync(StringCodec.UTF8, any())
        } throws RuntimeException("Nooo")

        val intake = RedisIntake(
            RedisConfiguration("localhost"),
            mockk()
        )

        assertThrows<RedisIntakeInitializationException> {
            runBlocking {
                intake.setup()
            }
        }

        unmockkAll()
    }

    @Test
    fun `Received messages will be aggregated if valid`() {
        val aggregateMock = mockk<AggregationManager>(relaxed = true)

        val intake = RedisIntake(
            RedisConfiguration("localhost"),
            aggregateMock
        )

        intake.message("channel", """
            {
                "project": "TestProject",
                "version": "v1.0.0",
                "severity": "INFO",
                "location": {
                    "file": "com.test.file",
                    "line": "10"
                },
                "timestamp": "2020-05-07T11:22:00.644Z"
            }
        """.trimIndent())

        coVerify(exactly = 1) {
            aggregateMock.aggregate(any())
        }
    }

    @Test
    fun `Invalid messages will be ignored, without crashes`() {
        val aggregateMock = mockk<AggregationManager>(relaxed = true)

        val intake = RedisIntake(
            RedisConfiguration("localhost"),
            aggregateMock
        )

        // Project is missing
        intake.message("channel", """
            {
                "version": "v1.0.0",
                "severity": "INFO",
                "location": {
                    "file": "com.test.file",
                    "line": "10"
                },
                "timestamp": "2020-05-07T11:22:00.644Z"
            }
        """.trimIndent())

        coVerify(exactly = 0) {
            aggregateMock.aggregate(any())
        }
    }
}
