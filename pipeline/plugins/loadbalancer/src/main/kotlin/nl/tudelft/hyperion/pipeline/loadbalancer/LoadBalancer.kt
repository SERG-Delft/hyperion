package nl.tudelft.hyperion.pipeline.loadbalancer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin
import nl.tudelft.hyperion.pipeline.PipelinePluginInitializationException
import org.zeromq.SocketType
import org.zeromq.ZContext
import kotlin.coroutines.CoroutineContext

/**
 * Basic load balancer that acts as both a plugin and plugin manager.
 * It distributes the input over multiple workers round-robin style
 * and collects the outputs on a single ZeroMQ socket.
 *
 * @property config the configuration to use
 */
class LoadBalancer(
    private val config: LoadBalancerPluginConfiguration
) : AbstractPipelinePlugin(config.pipeline) {

    override fun run(context: CoroutineContext) = CoroutineScope(Dispatchers.Default).launch {
        if (!hasConnectionInformation) {
            throw PipelinePluginInitializationException("Cannot run plugin without connection information")
        }

        // start plugin manager worker
        val pluginManagerWorker = WorkerManager.run(
            config.workerManagerHostname,
            config.workerManagerPort,
            if (canReceive) config.sinkPort else null,
            if (canSend) config.ventilatorPort else null
        )

        // start sender proxy thread, if we can send
        val senderThread = if (canSend) {
            createSenderProxyThread()
        } else {
            null
        }
        senderThread?.start()

        // start receiver proxy thread, if we can receive
        val receiverThread = if (canReceive) {
            createReceiverProxyThread()
        } else {
            null
        }
        receiverThread?.start()

        // Sleep while workers are active
        try {
            pluginManagerWorker.join()
        } finally {
            // cancel and cleanup
            pluginManagerWorker.cancelAndJoin()
            senderThread?.interrupt()
            receiverThread?.interrupt()
        }
    }

    /**
     * Creates a new thread that will proxy from our receiving port to the
     * ventilator used for our child workers.
     */
    fun createReceiverProxyThread() = Thread {
        ZContext().use {
            val incoming = it.createSocket(SocketType.PULL)
            val outgoing = it.createSocket(SocketType.PUSH)

            if (subConnectionInformation.isBind) {
                incoming.bind(subConnectionInformation.host)
            } else {
                incoming.connect(subConnectionInformation.host)
            }

            outgoing.bind(createAddress(config.workerManagerHostname, config.ventilatorPort))

            zmq.ZMQ.proxy(incoming.base(), outgoing.base(), null)
        }
    }

    /**
     * Creates a new thread that will proxy from our sink port to the
     * next step in the pipeline.
     */
    fun createSenderProxyThread() = Thread {
        ZContext().use {
            val incoming = it.createSocket(SocketType.PULL)
            val outgoing = it.createSocket(SocketType.PUSH)

            incoming.bind(createAddress(config.workerManagerHostname, config.sinkPort))

            if (pubConnectionInformation.isBind) {
                outgoing.bind(pubConnectionInformation.host)
            } else {
                outgoing.connect(pubConnectionInformation.host)
            }

            zmq.ZMQ.proxy(incoming.base(), outgoing.base(), null)
        }
    }

    override suspend fun onMessageReceived(msg: String) {
        // never invoked as we override run to never create a listener
    }
}

/**
 * Helper function to create a tcp address string.
 */
fun createAddress(hostname: String, port: Int) = "tcp://${hostname}:${port}"
