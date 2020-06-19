# Hyperion - Rate Plugin

![Gradle check pipeline rate plugin](https://github.com/SERG-Delft/hyperion/workflows/Gradle%20check%20pipeline%20rate%20plugin/badge.svg)

This package provides a rate pipeline plugin that can be used to monitor the rate at which messages are passing through the pipeline. The main goal of this plugin is to serve as a benchmarking tool or for debugging purposes.

> **Note**: The rate plugin works as both a passthrough plugin (forwarding all received messages to the next stage) and as a final step in the pipeline.

## Usage

_For full details on the supported configuration format, please see the [configuration section](#Configuration) of this document_.

The rate plugin simply takes whatever input it gets, calculates the amount of messages received each minute and logs the rate every so often. In the meantime every message is simply forwarded as is. How often the rate is logged can be configured.

For example, given the following (partial) configuration:

```yaml
rate: 5
```

The plugin will log the rate every five seconds. By default it will do this every 10 seconds.

## Building & Running

To build the library, run `gradle pipeline:rate:shadowJar`. The result will be located in `build/rate-all.jar`.

To execute the tests and linting, run `gradle pipeline:rate:check`.

To run a compiled version of the rate plugin, simply launch it using Java:

```shell script
java -jar build/rate-all.jar [path to config]
```

## Docker

If you have a built version of this plugin, you can use the accompanied Dockerfile to set up an image with all pre-requisites installed. Please note that this Dockerfile will not compile the plugin for you.

Please note that the docker container for this plugin will load the configuration file from `/root/config.yml` in its container.

A pre-built image is available at dockerhub under `daveter9/hyperion-pipeline-plugins-rate:0.1.0`.
To run this image with `rate_config.yml` as its configuration execute:

```shell script
docker run -it -rm -v ${PWD}/rate_config.yml:/root/config.yml daveter9/hyperion-pipeline-plugins-rate:0.1.0
```

## Configuration

This plugin accepts configuration in a YAML file supplied as a command line argument. The following options are accepted:

```yaml
# The rate at which rates are logged. In this case every 5 seconds.
rate: 5
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
    plugin-id: "RateLogger"
  
    # The size of the internal buffer used for storing data that has not yet been processed
    # locally. Increasing this will allow for more messages to be buffered, at the cost of
    # more memory usage. Messages incoming while the buffer is full will be thrown away. If
    # this happens often, consider using the load balancer plugin to shard this plugin across
    # multiple instances. Defaults to 20,000.
    buffer-size: 20000
```

## Input Format

This plugin will accept essentially any string at all.

```json
{
    "message": "[Apr 10] INFO com.foo.Bar:10 - Message"
}
```

## Output Format

The plugin will simply forward whatever input it got after counting it.

```json
{
    "message": "[Apr 10] INFO com.foo.Bar:10 - Message",
}
```
