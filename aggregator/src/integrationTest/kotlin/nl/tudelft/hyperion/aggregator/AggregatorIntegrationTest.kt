package nl.tudelft.hyperion.aggregator

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.joda.time.DateTime
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files

@Testcontainers
class AggregatorIntegrationTest {
    @Container
    private val redisContainer = KGenericContainer("redis:6.0-alpine")
        .withExposedPorts(6379)

    @Container
    private val postgresContainer = KGenericContainer("postgres:12.0-alpine")
        .withExposedPorts(5432)
        .withEnv("POSTGRES_PASSWORD", "mysecretpassword")

    @Test
    fun testMain(): Unit = runBlocking {
        // Step 1: Write a config.
        val postgresPath = "${postgresContainer.containerIpAddress}:${postgresContainer.getMappedPort(5432)}"

        val temporaryFile = File.createTempFile("hyperion-aggregator-config", "yaml")
        Files.writeString(
            temporaryFile.toPath(), """
                databaseUrl: "postgresql://$postgresPath/postgres?user=postgres&password=mysecretpassword"
                port: 38173
                granularity: 1 # 1 second
                aggregationTtl: 604800 # 7 days
                redis:
                    host: ${redisContainer.containerIpAddress}
                    port: ${redisContainer.getMappedPort(6379)}
            """.trimIndent()
        )

        // Step 2: Create a redis client so we can interface with the pipeline
        val redis = RedisClient.create(
            RedisURI.create(redisContainer.containerIpAddress, redisContainer.getMappedPort(6379))
        ).connect().sync()

        // Step 3: Prepare redis environment.
        redis.hset("Aggregator-config", "subChannel", "incoming")

        // Step 4: Start the aggregator and issue some commands.
        val aggregator = coMain(temporaryFile.absolutePath)
        delay(1000L)

        // Step 5: submit a couple of aggregation log entries
        for (i in 0..5) {
            redis.publish(
                "incoming",
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
        redis.publish(
            "incoming", """
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
        redis.publish(
            "incoming",
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
        redis.publish(
            "incoming",
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
                [{"interval":1,"versions":{}},{"interval":10,"versions":{}},{"interval":100,"versions":{}}]
            """.trimIndent(),
            content
        )

        // Step 6: wait a second or two to allow the submitted entries to aggregate
        delay(1000L)

        // Step 7: assert that the content was aggregated
        val (aggregatedStatus, aggregatedContent) = doRequest(
            "http://localhost:38173/api/v1/metrics?project=TestProject&file=com.test.file&intervals=0,1,10"
        )

        Assertions.assertEquals(200, aggregatedStatus)
        Assertions.assertEquals(
            """
                [{"interval":1,"versions":{"v1.0.0":[{"line":10,"severity":"INFO","count":4},{"line":20,"severity":"DEBUG",
                "count":1}],"v1.1.0":[{"line":10,"severity":"INFO","count":3}]}},{"interval":1,"versions":{"v1.0.0":[{"line
                ":10,"severity":"INFO","count":4},{"line":20,"severity":"DEBUG","count":1}],"v1.1.0":[{"line":10,"severity
                ":"INFO","count":3}]}},{"interval":10,"versions":{"v1.0.0":[{"line":10,"severity":"INFO","count":4},{"line
                ":20,"severity":"DEBUG","count":1}],"v1.1.0":[{"line":10,"severity":"INFO","count":3}]}}]
            """.trimIndent().replace("\n", ""),
            aggregatedContent
        )

        // Step 8: cleanup
        aggregator.cancelAndJoin()
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
class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)
