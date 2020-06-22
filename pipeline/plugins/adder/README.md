# Hyperion - Adder Plugin

![Gradle check pipeline adder plugin](https://github.com/SERG-Delft/hyperion/workflows/Gradle%20check%20pipeline%20adder%20plugin/badge.svg)

This package provides an adder pipeline plugin that is able to add fixed values to the incoming log messages.

## Usage

_For full details on the supported configuration format, please see the [configuration section](#Configuration) of this document_.

The adder plugin simply adds values to incoming JSON blobs, unless there is already a value with the same name. It can be used for fields that don't appear in your input source and have a constant value, such as the `project` field in case you only have a single project.

Take the following (partial) example configuration:

```yaml
add:
  - key: "animals.chicken"
    value: "egg"

  - key: "low"
    value: "high"
    overwrite-null: true
```

If given the following input:

```json
{
  "foo": "bar"
}
```

The adder plugin will add the `"egg"` literal in the `animals.chicken` field, and the `"high"` literal in the `low` field, resulting in the following:

```json
{
  "foo": "bar",
  "animal": {
    "chicken": "egg"
  },
  "low": "high"
}
```

The adder plugin will never overwrite existing values, except if the key exists but has a `null` value. In that case, whether or not the adder will overwrite the field is determined by the `overwrite-null` setting, which is false by default.

For example, for the following input:

```json
{
  "animal": {
    "chicken": null
  },
  "low": null
}
```

The adder will generate the following output:

```json
{
  "animal": {
    "chicken": null
  },
  "low": "high"
}
```

## Building & Running

To build the library, run `gradle pipeline:adder:shadowJar`. The result will be located in `build/adder-all.jar`.

To execute the tests and linting, run `gradle pipeline:adder:check`.

To run a compiled version of the adder plugin, simply launch it using Java:

```shell script
java -jar build/adder-all.jar [path to config]
```

## Docker
The adder plugin can be easily build and run using [Docker](https://www.docker.com/). 

### Running the pre-built docker image
A pre-built image is available at the [docker hub repository](https://hub.docker.com/r/sergdelft/hyperion).
The tag to use is `sergdelft/hyperion:pipeline-plugins-adder-0.1.0`, for the latest version please check the repository.

To run this image with `adder_config.yml` as its configuration execute:
```shell script
docker run -it -rm -v ${PWD}/adder_config.yml:/root/config.yml sergdelft/hyperion:pipeline-plugins-adder-0.1.0
```

### Building the docker image yourself
The included Dockerfile compiles the adder plugin into a fat jar and copies it to a new image which runs the plugin with the given config.
To build and run the plugin, execute the following command from the _project root_. 

```shell script
docker build . -f pipeline/plugins/adder/Dockerfile -t hyperion-adder:latest
```

after building is complete you can run the adder.
Please note that the docker container will load the configuration file from `/root/config.yml` in its container.

```shell script
docker run -it -rm -v ${PWD}/adder_config.yml:/root/config.yml hyperion-adder:latest
```

## Configuration

> **Note**: Unlike many other pipeline plugins, the adder plugin is able to watch the configuration file for changes and automatically update without the need to restart. This allows you to write to the file dynamically when you want to change a value, without the need to write a completely custom plugin.

This plugin accepts configuration in a YAML file supplied as a command line argument. The following options are accepted:

```yaml
# A list of key-value pairs that should be added to the incoming
# JSON blobs. Each entry must have a (key, value) pair as described
# below. An arbitrary amount of adders are supported.
add:
  -
    # The name of the field that needs to be added. This can be a nested
    # expression delimited by dots, in which case it will automatically
    # create child objects as needed.
    key: "my.field"
    # The string value to put in the field. This value will be added 
    # as long as the incoming message does not already contain a value
    # for the field.
    value: "foo"
    # Whether or not an explicit "null" value in the incoming JSON should
    # be overwritten with the value given in `value`. If set to false, will
    # leave the null value intact. If set to true, will overwrite it. This
    # field is false by default.
    overwrite-null: true
    
  # Add more entries as necessary...
  -
    key: "my.other.field"
    # etc.

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
    plugin-id: "Adder"
  
    # The size of the internal buffer used for storing data that has not yet been processed
    # locally. Increasing this will allow for more messages to be buffered, at the cost of
    # more memory usage. Messages incoming while the buffer is full will be thrown away. If
    # this happens often, consider using the load balancer plugin to shard this plugin across
    # multiple instances. Defaults to 20,000.
    buffer-size: 20000
```

## Input Format

This plugin accepts any type of JSON value as input. If the input is not valid JSON, or if it is not a JSON object, it will be passed to the next stage of the pipeline unaffected. Other than that, any type of JSON object is accepted.

```json
{
    "message": "[Apr 10] INFO com.foo.Bar:10 - Message"
}
```

## Output Format

This plugin will transform the incoming JSON message according to the configuration and output a new JSON object that strictly contains the same or more values than the input. For the example given in the usage section, the output is as follows:

```json
{
    "message": "[Apr 10] INFO com.foo.Bar:10 - Message",
    "animal": {
        "chicken": "egg"
    },
    "low": "high"
}
```
