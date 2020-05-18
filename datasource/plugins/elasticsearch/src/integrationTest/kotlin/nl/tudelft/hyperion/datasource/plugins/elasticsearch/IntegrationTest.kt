package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder
import org.elasticsearch.client.Request
import org.elasticsearch.client.RestClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.zeromq.SocketType
import org.zeromq.ZContext
import java.util.*
import java.util.concurrent.Executors

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTest {

    private lateinit var esContainer: ElasticsearchContainer
    private lateinit var testClient: RestClient
    private val workers: MutableList<Job> = mutableListOf()

    private val managerPort = 30_000
    private val receiverPort = 30_001

    private val receiverChannel = Channel<String>(1000)

    private val mockPluginManager = object {
        fun manager() = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()).launch {
            val ctx = ZContext()
            val sock = ctx.createSocket(SocketType.REP)
            sock.bind("tcp://*:${managerPort}")

            while (isActive) {
                sock.recvStr()
                sock.send("""{"host": "tcp://localhost:${receiverPort}", "isBind": "false"}""")
            }

            sock.close()
            ctx.close()
        }

        fun receiver() = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()).launch {
            val ctx = ZContext()
            val sock = ctx.createSocket(SocketType.PULL)
            sock.bind("tcp://*:${receiverPort}")

            while (isActive) {
                receiverChannel.send(sock.recvStr())
            }

            sock.close()
            ctx.close()
        }
    }

    @BeforeAll
    fun setUp() {
        esContainer = ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.7.0")
        esContainer.start()

        val credentialsProvider: CredentialsProvider = BasicCredentialsProvider()
        credentialsProvider.setCredentials(AuthScope.ANY, UsernamePasswordCredentials("elastic", "changeme"))
        testClient = RestClient.builder(HttpHost.create(esContainer.httpHostAddress))
                .setHttpClientConfigCallback { httpClientBuilder: HttpAsyncClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                }
                .build()

        // create index
        testClient.performRequest(Request("PUT", "/logs"))

        // create document
        val request = Request("POST", "/logs/_doc")
        request.setJsonEntity("""{"timestamp":"${Date()}", "log": "INFO /src/foo.py 32 - bar"}""")
        testClient.performRequest(request)

        workers.add(mockPluginManager.manager())
        workers.add(mockPluginManager.receiver())
    }

    @AfterAll
    fun teardown() {
        esContainer.stop()
        runBlocking {
            workers.map { it.cancel() }
        }
    }

    @Test
    fun `Test all components`() {
        assertTrue(true)
    }

}