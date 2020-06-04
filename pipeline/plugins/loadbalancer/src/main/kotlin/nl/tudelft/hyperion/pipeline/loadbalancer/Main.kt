package nl.tudelft.hyperion.pipeline.loadbalancer

import nl.tudelft.hyperion.pipeline.runPipelinePlugin

fun main(vararg args: String) = runPipelinePlugin(
    args.firstOrNull() ?: "./config.yaml",
    ::LoadBalancer
)
