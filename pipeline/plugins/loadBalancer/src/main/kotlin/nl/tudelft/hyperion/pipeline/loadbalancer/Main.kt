package nl.tudelft.hyperion.pipeline.loadbalancer

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import nl.tudelft.hyperion.pipeline.PipelinePluginInitializationException
import nl.tudelft.hyperion.pipeline.runPipelinePlugin
import org.zeromq.SocketType
import org.zeromq.ZContext
import java.util.concurrent.Executors

class LoadBalancer(config: LoadBalancerPluginConfiguration) : AbstractPipelinePlugin(config.zmq) {

    // TODO: Change from hard coded
    private val hostname = "localhost"
    private val managerPort = 5565
    private val ventilatorPort = 5575
    private val sinkPort = 5585

    override fun run() = GlobalScope.launch {
        if (!hasConnectionInformation) {
            throw PipelinePluginInitializationException("Cannot run plugin without connection information")
        }

        val inputChannel = Channel<String>(capacity = 20_000)
        val outputChannel = Channel<String>(capacity = 20_000)

        val jobs = listOf(
            WorkerManager.run(hostname, managerPort, sinkPort, ventilatorPort),
            runReceiver(inputChannel),
            createChannelPass(hostname, ventilatorPort, inputChannel, SocketType.PUSH),
            createChannelPass(hostname, sinkPort, outputChannel, SocketType.PULL),
            runSender(outputChannel)
        )

        // Sleep infinitely
        try {
            while (isActive) {
                delay(Long.MAX_VALUE)
            }
        } finally {
            jobs.map { it.cancelAndJoin() }
        }
    }

    private fun createChannelPass(
            hostname: String,
            port: Int, channel:
            Channel<String>,
            socketType: SocketType
    ) = run {
        val coroutineScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
        val ctx = ZContext()
        val sock = ctx.createSocket(socketType)
        sock.bind(createAddress(hostname, port))

        coroutineScope.launch {
            while (isActive) {
                sock.send(channel.receive())
            }
        }
    }

    override suspend fun process(input: String): String? {
        return input
    }
}

fun createAddress(hostname: String, port: Int) = "tcp://${hostname}:${port}"

fun main(vararg args: String) = runPipelinePlugin(
    args.firstOrNull() ?: "./config.yaml",
    ::LoadBalancer
)
