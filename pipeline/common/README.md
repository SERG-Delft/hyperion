# Hyperion - Pipeline Commons

![Gradle check pipeline common](https://github.com/SERG-Delft/monitoring-aware-ides/workflows/Gradle%20check%20pipeline%20common/badge.svg)

This package contains a common abstract implementation in Kotlin for pipeline plugin implementation. It is intended to be used as a library for plugin implementations to link against.

For instructions on how to create a new plugin using this package, please see the following documentation article:

- [Writing a new pipeline plugin in Java/Kotlin](/docs/writing-java-kotlin-plugin.md)

## Building & Running

To build the library, run `gradle pipeline:common:shadowJar`. The result will be located in `build/common-all.jar`.

To execute the tests and linting, run `gradle pipeline:common:check`.

## Configuration

The abstract plugin defined in this package requires a common configuration defining the location of the plugin manager and the ID of the host. The configuration looks like this in YAML format:

```yaml
# Various settings needed for the plugin to interact with the pipeline,
# such as it's unique ID and the hostname and port of the Hyperion plugin manager.
# 
# Please note that the plugin must also be able to talk to any of its previous
# and next steps in the pipeline. As such, it is recommended that all of the 
# plugins are contained on a single networking setup.
pipeline:
    # The host and port pair that can be used to contact the Hyperion plugin manager.
    # Please note that this machine must be able to talk over TCP to the manager and
    # that the manager must be aware of this plugin/aggregator.
    manager-host: "manager:8000"
  
    # The unique ID of this pipeline step that matches the configuration of the plugin
    # manager. Used to identify which plugins are inputs/outputs of this step. Please
    # note that the plugin will crash at launch if the plugin manager does not recognize
    # this plugin ID.
    plugin-id: "MyPlugin"
  
    # The size of the internal buffer used for storing data that has not yet been processed
    # locally. Increasing this will allow for more messages to be buffered, at the cost of
    # more memory usage. Messages incoming while the buffer is full will be thrown away. If
    # this happens often, consider using the load balancer plugin to shard this plugin across
    # multiple instances.
    buffer-size: 20000
```
