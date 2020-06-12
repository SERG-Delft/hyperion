package nl.tudelft.hyperion.aggregator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.zeromq.SocketType
import org.zeromq.ZContext
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.util.concurrent.Executors

class AggregatorIntegrationTest {
    @Test
    fun testMain(): Unit = runBlocking {
        val postgres = KPostgreSQLContainer()
        postgres.start()

        // Step 1: Write a config.
        val postgresPath = "${postgres.containerIpAddress}:${postgres.getMappedPort(5432)}"
        val postgresUrl = "postgresql://$postgresPath/postgres?user=${postgres.username}&password=${postgres.password}"

        val temporaryFile = File.createTempFile("hyperion-aggregator-config", "yaml")
        Files.writeString(
            temporaryFile.toPath(), """
                database-url: "$postgresUrl"
                port: 38173
                granularity: 5 # 5 seconds
                aggregation-ttl: 604800 # 7 days
                pipeline:
                    manager-host: localhost:39181
                    plugin-id: Aggregator
            """.trimIndent()
        )

        // Step 2: Create a "plugin manager" that will respond with a static channel.
        val pluginManager = runDummyZMQPluginServer(
            39181, """
            {"host":"tcp://localhost:39182","isBind":false}
        """.trimIndent()
        )

        // Step 3: Create a ZMQ pusher that will send messages to the aggregator.
        val (pusher, channel) = runDummyZMQPublisher(39182)

        // Step 4: Start the aggregator and issue some commands.
        val aggregator = coMain(temporaryFile.absolutePath)
        delay(7000L) // ensure that we're at the start of a new granularity slot

        // Step 5: submit a couple of aggregation log entries
        for (i in 0..5) {
            channel.send(
                """
                    {
                        "project": "TestProject",
                        "version": "${if (i < 3) "v1.0.0" else "v1.1.0"}",
                        "severity": "INFO",
                        "location": {
                            "file": "com.test.file",
                            "line": 10
                        },
                        "timestamp": "${DateTime.now()}"
                    }
                """.trimIndent()
            )
        }

        // Submit an invalid one, should be ignored.
        channel.send(
            """
                {
                    "version": "v1.0.0",
                    "severity": "INFO",
                    "location": {
                        "file": "com.test.file",
                        "line": 10
                    },
                    "timestamp": "${DateTime.now()}"
                }
            """.trimIndent()
        )

        // One with a wrong severity. Should still be counted.
        channel.send(
            """
                {
                    "project": "TestProject",
                    "version": "v1.0.0",
                    "severity": "WARN",
                    "location": {
                        "file": "com.test.file",
                        "line": 10
                    },
                    "timestamp": "${DateTime.now()}"
                }
            """.trimIndent()
        )

        // One on a different line.
        channel.send(
            """
                {
                    "project": "TestProject",
                    "version": "v1.0.0",
                    "severity": "DEBUG",
                    "location": {
                        "file": "com.test.file",
                        "line": 20
                    },
                    "timestamp": "${DateTime.now()}"
                }
            """.trimIndent()
        )

        // Step 6: Ensure that our current aggregation is empty.
        val (status, content) = doRequest(
            "http://localhost:38173/api/v1/metrics?project=TestProject&file=com.test.file&intervals=1,10,100"
        )

        Assertions.assertEquals(200, status)
        Assertions.assertEquals(
            """
                [{"interval":5,"versions":{}},{"interval":10,"versions":{}},{"interval":100,"versions":{}}]
            """.trimIndent(),
            content
        )

        // Step 6: wait a second or two to allow the submitted entries to aggregate
        delay(5000L)

        // Step 7: assert that the content was aggregated
        val (aggregatedStatus, aggregatedContent) = doRequest(
            "http://localhost:38173/api/v1/metrics?project=TestProject&file=com.test.file&intervals=5,10"
        )

        Assertions.assertEquals(200, aggregatedStatus)
        Assertions.assertEquals(
            """
                [{"interval":5,"versions":{"v1.0.0":[{"line":10,"severity":"INFO","count":4},{"line":20,"severity":"DEBU
                G","count":1}],"v1.1.0":[{"line":10,"severity":"INFO","count":3}]}},{"interval":10,"versions":{"v1.0.0":
                [{"line":10,"severity":"INFO","count":4},{"line":20,"severity":"DEBUG","count":1}],"v1.1.0":[{"line":10,
                "severity":"INFO","count":3}]}}]
            """.trimIndent().replace("\n", ""),
            aggregatedContent
        )

        // Step 8: cleanup
        aggregator.cancelAndJoin()
        pusher.cancelAndJoin()
    }

    /**
     * Helper function that creates a new ZMQ pusher. Returns the job (for
     * cancelling) and a channel that can be used to publish messages.
     */
    private fun runDummyZMQPublisher(port: Int): Pair<Job, Channel<String>> {
        val channel = Channel<String>()

        return Pair(
            CoroutineScope(
                Executors.newSingleThreadExecutor().asCoroutineDispatcher()
            ).launch {
                val ctx = ZContext()
                val sock = ctx.createSocket(SocketType.PUSH)
                sock.bind("tcp://*:$port")

                while (isActive) {
                    sock.send(channel.receive())
                }

                sock.close()
                ctx.destroy()
            },
            channel
        )
    }

    /**
     * Helper function that starts a new ZMQ plugin server that always returns the
     * same content for every request.
     */
    private fun runDummyZMQPluginServer(port: Int, respondWith: String) = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    ).launch {
        val ctx = ZContext()
        val sock = ctx.createSocket(SocketType.REP)
        sock.bind("tcp://*:$port")

        while (isActive) {
            sock.recvStr()
            sock.send(respondWith)
        }

        sock.close()
        ctx.destroy()
    }

    /**
     * Helper function to make an http request to the specified location.
     * Returns a pair of the status code and return text.
     */
    private suspend fun doRequest(path: String): Pair<Int, String> {
        val request = HttpRequest
            .newBuilder()
            .uri(URI(path))
            .GET()
            .build()

        val response = HttpClient
            .newHttpClient()
            .sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .await()

        return Pair(response.statusCode(), response.body())
    }
}

// Fix for TestContainers doing some weird java stuff that Kotlin doesn't like.
class KPostgreSQLContainer : PostgreSQLContainer<KPostgreSQLContainer>()

