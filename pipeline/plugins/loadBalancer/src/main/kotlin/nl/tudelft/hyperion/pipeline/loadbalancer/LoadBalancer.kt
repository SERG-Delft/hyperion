package nl.tudelft.hyperion.pipeline.loadbalancer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import nl.tudelft.hyperion.pipeline.PipelinePluginInitializationException
import org.zeromq.SocketType
import org.zeromq.ZContext
import java.util.concurrent.Executors

/**
 * Basic load balancer that acts as both a plugin and plugin manager.
 * It distributes the input over multiple workers and collects the outputs
 * on a single ZeroMQ socket.
 *
 * @property config the configuration to use
 */
class LoadBalancer(
        private val config: LoadBalancerPluginConfiguration
) : AbstractPipelinePlugin(config.zmq) {

    override fun run() = GlobalScope.launch {
        if (!hasConnectionInformation) {
            throw PipelinePluginInitializationException("Cannot run plugin without connection information")
        }

        // create channels for passing messages
        val inputChannel = Channel<String>(capacity = config.zmq.bufferSize)
        val outputChannel = Channel<String>(capacity = config.zmq.bufferSize)

        // create separate worker threads for the ventilator and sink
        val inputWorkerScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
        val outputWorkerScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

        // start workers
        val parent = coroutineScope {
            WorkerManager.run(
                    config.workerManagerHostname,
                    config.workerManagerPort,
                    config.sinkPort,
                    config.ventilatorPort
            )
            runReceiver(inputChannel)
            inputWorkerScope.createChannelPass(
                    config.workerManagerHostname,
                    config.ventilatorPort,
                    inputChannel,
                    SocketType.PUSH
            )
            outputWorkerScope.createChannelPass(
                    config.workerManagerHostname,
                    config.sinkPort,
                    outputChannel,
                    SocketType.PULL
            )
            runSender(outputChannel)
        }

        // Sleep while workers are active
        try {
            parent.join()
        } finally {
            parent.cancelAndJoin()
        }
    }

    /**
     * Starts a Job that sends messages from the given channel
     * to a ZeroMQ socket.
     *
     * @param hostname hostname to bind the socket to
     * @param port port to bind the socket to
     * @param channel the channel to receive messages from
     * @param socketType defines what type of socket to use
     */
    private fun CoroutineScope.createChannelPass(
            hostname: String,
            port: Int,
            channel: Channel<String>,
            socketType: SocketType
    ) = launch {
        ZContext().use {
            val sock = it.createSocket(socketType)
            sock.bind(createAddress(hostname, port))

            while (isActive) {
                sock.send(channel.receive())
            }
        }
    }

    override suspend fun process(input: String): String? {
        return input
    }
}

/**
 * Helper function to create a tcp address string.
 */
fun createAddress(hostname: String, port: Int) = "tcp://${hostname}:${port}"
