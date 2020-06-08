# Hyperion - Reader Plugin

![Gradle check pipeline reader plugin](https://github.com/SERG-Delft/monitoring-aware-ides/workflows/Gradle%20check%20pipeline%20reader%20plugin/badge.svg)

This package provides a reader pipeline plugin that is able to read messages from stdin and publish it to a Hyperion pipeline. This plugin is primarily intended for debugging and should not be used for production purposes.

## Usage

_For full details on the supported configuration format, please see the [configuration section](#Configuration) of this document_.

After launching the plugin, all messages entered in stdin will automatically be published to the pipeline as soon as a newline is encountered.

## Building & Running

To build the library, run `gradle pipeline:reader:shadowJar`. The result will be located in `build/reader-all.jar`.

To execute the tests and linting, run `gradle pipeline:reader:check`.

To run a compiled version of the reader plugin, simply launch it using Java:

```shell script
java -jar build/reader-all.jar [path to config]
```

## Docker

If you have a built version of this plugin, you can use the accompanied Dockerfile to set up an image with all pre-requisites installed. Please note that this Dockerfile will not compile the plugin for you.

Also note that the docker container for this plugin will load the configuration file from the `CONFIGFILE` environment variable, and not the command line arguments.

## Configuration

This plugin accepts configuration in a YAML file supplied as a command line argument. The following options are accepted:

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
    plugin-id: "Reader"
  
    # The size of the internal buffer used for storing data that has not yet been processed
    # locally. Increasing this will allow for more messages to be buffered, at the cost of
    # more memory usage. Messages incoming while the buffer is full will be thrown away. If
    # this happens often, consider using the load balancer plugin to shard this plugin across
    # multiple instances. Defaults to 20,000.
    buffer-size: 20000
```

## Output Format

The plugin will simply forward all input entered in stdin. Note that further plugins in the pipeline may expect input in a specific format (likely JSON).
