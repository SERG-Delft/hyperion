package nl.tudelft.hyperion.pipeline.loadbalancer

import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * The full configuration with all necessary parameters to
 * run the load balancer plugin.
 *
 * @property zmq the necessary arguments to set up ZeroMQ and connect to
 *  the plugin manager
 * @property workerManagerHostname the hostname to bind the worker manager to
 * @property workerManagerPort the REQ/REP port to bind the worker manager to
 * @property ventilatorPort the port to bind a PUSH socket to, for sending messages
 *  to the PULL sockets of the workers
 * @property sinkPort the port to bind a PULL socket to, for collecting the
 *  processed messages from the PUSH sockets of the workers
 */
data class LoadBalancerPluginConfiguration(
        val zmq: PipelinePluginConfiguration,
        val workerManagerHostname: String,
        val workerManagerPort: Int,
        val ventilatorPort: Int,
        val sinkPort: Int
)
