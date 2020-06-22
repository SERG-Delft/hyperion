# Hyperion - Printer Plugin

![Gradle check pipeline printer plugin](https://github.com/SERG-Delft/hyperion/workflows/Gradle%20check%20pipeline%20printer%20plugin/badge.svg)

This package provides a printer pipeline plugin that can be used to print all messages passing through a part of the pipeline. This plugin may be useful for debugging purposes.

> **Note**: The printer plugin works as both a passthrough plugin (forwarding all received messages to the next stage) and as a final step in the pipeline.

## Usage

_For full details on the supported configuration format, please see the [configuration section](#Configuration) of this document_.

The printer plugin simply takes whatever input it gets, prints it, and forwards it. Because this behaviour is static, only the configuration for the abstract pipeline plugin is needed.

## Building & Running

To build the library, run `gradle pipeline:printer:shadowJar`. The result will be located in `build/printer-all.jar`.

To execute the tests and linting, run `gradle pipeline:printer:check`.

To run a compiled version of the printer plugin, simply launch it using Java:

```shell script
java -jar build/printer-all.jar [path to config]
```

## Docker
The printer plugin can be easily build and run using [Docker](https://www.docker.com/). 

### Running the pre-built docker image
A pre-built image is available at the [docker hub repository](https://hub.docker.com/r/sergdelft/hyperion).
The tag to use is `sergdelft/hyperion:pipeline-plugins-printer-0.1.0`, for the latest version please check the repository.

To run this image with `printer_config.yml` as its configuration execute:
```shell script
docker run -it -rm -v ${PWD}/printer_config.yml:/root/config.yml sergdelft/hyperion:pipeline-plugins-printer-0.1.0
```

### Building the docker image yourself
The included Dockerfile compiles the printer plugin into a fat jar and copies it to a new image which runs the plugin with the given config.
To build and run the plugin, execute the following command from the _project root_. 

```shell script
docker build . -f pipeline/plugins/printer/Dockerfile -t hyperion-printer:latest
```

after building is complete you can run the printer.
Please note that the docker container will load the configuration file from `/root/config.yml` in its container.

```shell script
docker run -it -rm -v ${PWD}/printer_config.yml:/root/config.yml hyperion-printer:latest
```

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
    plugin-id: "Printer"
  
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

The plugin will simply forward whatever input it got after printing it.

```json
{
    "message": "[Apr 10] INFO com.foo.Bar:10 - Message",
}
```
