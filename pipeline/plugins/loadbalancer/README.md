# Hyperion - Load Balancer Plugin

![Gradle check pipeline loadbalancer plugin](https://github.com/SERG-Delft/monitoring-aware-ides/workflows/Gradle%20check%20pipeline%20loadbalancer%20plugin/badge.svg)

This package provides a load balancer plugin implementation that is able to distribute incoming messages evenly between several child plugin workers in a transparent fashion. Unlike other Hyperion plugins, this plugin provides no features on its own and is instead only responsible for delegating messages to child workers.

## Usage

_For full details on the supported configuration format, please see the [configuration section](#Configuration) of this document_.

The load balancer plugin essentially acts as a fake plugin manager to its children. By faking the plugin manager, any plugin that works with the real pipeline also automatically works in the load balancer. 

To set up the load balancer plugin, first configure the hosts and ports it will use for the plugin manager instance and the distributing nodes:

```yaml
worker-manager-hostname: localhost
worker-manager-port: 5555

ventilator-port: 3000
sink-port: 4000
```

With this configuration, the load balancer will host a fake plugin manager on localhost:5555. It will also host a ventilator and sink at ports 3000 and 4000 respectively, as per the divide-and-conquer pattern from the official ZeroMQ [guide](http://zguide.zeromq.org/page:all#Divide-and-Conquer).

To set up worker plugins, simply configure them as normal but point them to the fake plugin manager instead of the real one. The load balancer plugin will automatically distribute traffic evenly in a round-robin fashion between the workers.

Visually, this setup looks like this:

![](https://i.imgur.com/FmM0WMm.png)

As you can see, to the real plugin manager the entire worker setup simply looks like a single plugin, making it transparent. Within the load balancer, all the workers are completely independent from the real pipeline plugin manager.

It should be noted that while the main use case of the load balancer plugin is to run multiple instances of the same worker type, you are also able to load balance between different plugin types. Obviously, this will lead to inconsistencies later down the line so it is not recommended to do so.

## Building & Running

To build the library, run `gradle pipeline:loadbalancer:shadowJar`. The result will be located in `build/loadbalancer-all.jar`.

To execute the tests and linting, run `gradle pipeline:loadbalancer:check`.

To run a compiled version of the load balancer plugin, simply launch it using Java:

```shell script
java -jar build/loadbalancer-all.jar [path to config]
```

## Docker

If you have a built version of this plugin, you can use the accompanied Dockerfile to set up an image with all pre-requisites installed. Please note that this Dockerfile will not compile the plugin for you.

Please note that the docker container for this plugin will load the configuration file from the `CONFIGFILE` environment variable, and not the command line arguments.

## Configuration

This plugin accepts configuration in a YAML file supplied as a command line argument. The following options are accepted:

```yaml
# The hostname the fake plugin manager should bind against. If set
# to 0.0.0.0 or *, will bind to all open interfaces.
worker-manager-hostname: localhost

# The port the plugin manager will bind against. Please note that child
# workers will need to be able to access this specific port.
worker-manager-port: 5555

# The port on which the ventilator component should run. Please note that
# child workers will need to be able to access this specific port.
ventilator-port: 3000

# The port on which the sink should run. Please note that child workers
# will need to be able to access this specific port.
sink-port: 4000

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
    plugin-id: "LoadBalancer"
  
    # The size of the internal buffer used for storing data that has not yet been processed
    # locally. Increasing this will allow for more messages to be buffered, at the cost of
    # more memory usage. Messages incoming while the buffer is full will be thrown away. If
    # this happens often, consider using the load balancer plugin to shard this plugin across
    # multiple instances. Defaults to 20,000.
    buffer-size: 20000
```

## Input Format

This plugin accepts the same input format as the worker plugins it hosts. Please refer to the documentation of the worker plugin for its accepted input format.

## Output Format

This plugin outputs the same format as the worker plugins it hosts. Please refer to the documentation of the worker plugin for its accepted output format.
