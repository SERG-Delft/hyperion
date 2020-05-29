# Load Balancer Plugin

A basic load balancer plugin that acts as both a plugin and plugin manager.
This allows for easily starting parallel plugins in the pipeline to handle a larger throughput.
A standard use case would be if the processing time of a plugin in the pipeline is acting as a bottleneck.

Any Hyperion supported plugin could theoretically be used as a worker, as the interface for the load balancer plugin is
identical to the plugin manager.

The load balancer can be ran by providing the path to a `.yml` config file.

## Example config file
```yaml
worker-manager-hostname: localhost
worker-manager-port: 5555
ventilator-port: 3000
sink-port: 4000

pipeline:
  plugin-id: LoadBalancer
  manager-host: "localhost:5000"
```

## Overview of the architecture

The plugin uses the divide-and-conquer pattern from the official ZeroMQ
[guide](http://zguide.zeromq.org/page:all#Divide-and-Conquer).

The following is a rough idea of how it is set up:

```
                                     worker1
+----------+        +------------+             +-----------       +--------+
| Receiver |        | Ventilator |   worker2   |   Sink   |       | Sender |
|          +------->+            |             |          +------>+        |
|  [H/C]   |        |   [Host]   |   worker3   |  [Host]  |       | [H/C]  |
+----------+        +------------+             +-----------       +--------+
                                      .....
```

Note: Whether the receiver and sender are host or client
depends on how the plugin manager directs the load balancer.