package nl.tudelft.hyperion.pipeline.loadbalancer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import nl.tudelft.hyperion.pipeline.PipelinePluginInitializationException
import org.zeromq.SocketType
import org.zeromq.ZContext
import java.util.concurrent.Executors

/**
 * Basic load balancer that acts as both a plugin and plugin manager.
 * It distributes the input over multiple workers round-robin style
 * and collects the outputs on a single ZeroMQ socket.
 *
 * @property config the configuration to use
 */
class LoadBalancer(
    private val config: LoadBalancerPluginConfiguration
) : AbstractPipelinePlugin(config.zmq) {

    override fun run() = CoroutineScope(Dispatchers.Default).launch {
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
        val parent = launch {
            WorkerManager.run(
                config.workerManagerHostname,
                config.workerManagerPort,
                config.sinkPort,
                config.ventilatorPort
            )
            runReceiver(inputChannel)
            inputWorkerScope.createChannelReceiver(
                config.workerManagerHostname,
                config.ventilatorPort,
                inputChannel
            )
            outputWorkerScope.createChannelSender(
                config.workerManagerHostname,
                config.sinkPort,
                outputChannel
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

    override suspend fun process(input: String): String? = input
}

/**
 * Starts a Job that sends messages from the given channel
 * with a PUSH ZeroMQ socket.
 *
 * @param hostname hostname to bind the socket to
 * @param port port to bind the socket to
 * @param channel the channel to receive messages from
 */
fun CoroutineScope.createChannelReceiver(
    hostname: String,
    port: Int,
    channel: Channel<String>
) = launch {
    ZContext().use {
        val sock = it.createSocket(SocketType.PUSH)
        sock.bind(createAddress(hostname, port))

        while (isActive) {
            sock.send(channel.receive())
        }
    }
}

/**
 * Starts a Job that sends messages to a channel from the
 * given ZeroMQ PULL socket.
 *
 * @param hostname hostname to bind the socket to
 * @param port port to bind the socket to
 * @param channel the channel to send messages from
 */
fun CoroutineScope.createChannelSender(
    hostname: String,
    port: Int,
    channel: Channel<String>
) = launch {
    ZContext().use {
        val sock = it.createSocket(SocketType.PULL)
        sock.bind(createAddress(hostname, port))

        while (isActive) {
            channel.send(sock.recvStr())
        }
    }
}

/**
 * Helper function to create a tcp address string.
 */
fun createAddress(hostname: String, port: Int) = "tcp://${hostname}:${port}"
