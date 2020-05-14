package nl.tudelft.hyperion.pipeline.loadbalancer

import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

data class LoadBalancerPluginConfiguration(
        val zmq: PipelinePluginConfiguration,
        val plugins: List<PluginInfo>,
        val ventilatorPort: Int,
        val managerPort: Int,
        val sinkPort: Int
)

data class PluginInfo(
        val host: String,
        val id: String
)