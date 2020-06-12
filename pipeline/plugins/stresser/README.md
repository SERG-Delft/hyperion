# Hyperion - Stresser Plugin

![Gradle check pipeline stresser plugin](https://github.com/SERG-Delft/hyperion/workflows/Gradle%20check%20pipeline%20stresser%20plugin/badge.svg)

This package provides a stress testing plugin that can be used to benchmark the performance of the Hyperion pipeline. It is designed to function as a data source that will simply hammer the next step in the pipeline with as much traffic as possible.

## Usage

_For full details on the supported configuration format, please see the [configuration section](#Configuration) of this document_.

Within the configuration, configure a message to be sent and optionally the amount of messages to be sent. On startup, the plugin will begin broadcasting as much as possible. Once the configured limit is reached (if set), the plugin will automatically shut down.

## Building & Running

To build the library, run `gradle pipeline:stresser:shadowJar`. The result will be located in `build/stresser-all.jar`.

To execute the tests and linting, run `gradle pipeline:stresser:check`.

To run a compiled version of the stresser plugin, simply launch it using Java:

```shell script
java -jar build/stresser-all.jar [path to config]
```

## Docker

If you have a built version of this plugin, you can use the accompanied Dockerfile to set up an image with all pre-requisites installed. Please note that this Dockerfile will not compile the plugin for you.

Also note that the docker container for this plugin will load the configuration file from the `CONFIGFILE` environment variable, and not the command line arguments.

## Configuration

This plugin accepts configuration in a YAML file supplied as a command line argument. The following options are accepted:

```yaml
# The message contents to be sent to the next plugin. If you're testing the performance
# of a specific plugin, this needs to be content that that plugin can accept. Also note
# that the throughput of the pipeline may vary based on the length of this message. It is
# therefore recommended that tests vary this message and observe the results.
message: "{\"message\":\"A log message!\"}"

# The number of times the specified message should be sent. This is an optional setting;
# leaving it out entirely will cause the plugin to instead infinitely send the message.
# If configured, the plugin will quit automatically after publishing the messages.
iterations: 10000

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
    plugin-id: "Stresser"
  
    # The size of the internal buffer used for storing data that has not yet been processed
    # locally. Increasing this will allow for more messages to be buffered, at the cost of
    # more memory usage. Messages incoming while the buffer is full will be thrown away. If
    # this happens often, consider using the load balancer plugin to shard this plugin across
    # multiple instances. Defaults to 20,000.
    buffer-size: 20000
```

## Output Format

This plugin will simply output the message specified in the `message` configuration key.
