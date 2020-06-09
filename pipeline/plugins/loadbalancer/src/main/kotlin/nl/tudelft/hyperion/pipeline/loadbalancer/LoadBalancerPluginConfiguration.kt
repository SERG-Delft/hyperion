package nl.tudelft.hyperion.pipeline.loadbalancer

import com.fasterxml.jackson.annotation.JsonProperty
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * The full configuration with all necessary parameters to
 * run the load balancer plugin.
 *
 * @property pipeline the necessary arguments to set up ZeroMQ and connect to
 *  the plugin manager
 * @property workerManagerHostname the hostname to bind the worker manager to
 * @property workerManagerPort the REQ/REP port to bind the worker manager to
 * @property ventilatorPort the port to bind a PUSH socket to, for sending messages
 *  to the PULL sockets of the workers
 * @property sinkPort the port to bind a PULL socket to, for collecting the
 *  processed messages from the PUSH sockets of the workers
 */
data class LoadBalancerPluginConfiguration(
    val pipeline: PipelinePluginConfiguration,
    @JsonProperty("worker-manager-hostname")
    val workerManagerHostname: String,
    @JsonProperty("worker-manager-port")
    val workerManagerPort: Int,
    @JsonProperty("ventilator-port")
    val ventilatorPort: Int,
    @JsonProperty("sink-port")
    val sinkPort: Int
)
