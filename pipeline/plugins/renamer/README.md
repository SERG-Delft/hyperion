# Hyperion - Rename Plugin

![Gradle check pipeline renamer plugin](https://github.com/SERG-Delft/hyperion/workflows/Gradle%20check%20pipeline%20renamer%20plugin/badge.svg)

This package provides a renamer plugin that is able to rename (nested) JSON fields in incoming messages.

## Usage

_For full details on the supported configuration format, please see the [configuration section](#Configuration) of this document_.

Using the rename plugin is very simple. Simply specify the list of fields that you want to rename:

```yaml
rename:
  - from: "log_line"
    to: "location.line"

  - from: "log_file"
    to: "location.file"

  - from: "does_not_exist"
    to: "exists"
```

When receiving a JSON input, this plugin will rename appropriately and leave any extra fields untouched. For example, given the following input:

```json
{
  "log_line": 10,
  "log_file": "MyFile.java",
  "extra": "Foo"
}
```

The renaming plugin will transform it into the following output:

```json
{
  "location": {
    "line": 10,
    "file": "MyFile.java"
  },
  "extra": "Foo"
}
```

The value of the field will remain intact, and as such any value can be renamed.

## Building & Running

To build the library, run `gradle pipeline:renamer:shadowJar`. The result will be located in `build/renamer-all.jar`.

To execute the tests and linting, run `gradle pipeline:renamer:check`.

To run a compiled version of the renamer plugin, simply launch it using Java:

```shell script
java -jar build/renamer-all.jar [path to config]
```

## Docker
The renamer plugin can be easily built and run using [Docker](https://www.docker.com/). 

### Running the pre-built Docker image
A pre-built image is available at the [Docker hub repository](https://hub.docker.com/r/sergdelft/hyperion).
The plugin image is tagged as `sergdelft/hyperion:pipeline-plugins-renamer-<version>`. Please consult the [root README](/README.md) for the latest published version.
To run this image with `renamer_config.yml` as its configuration execute:
```shell script
docker run -it -rm -v ${PWD}/renamer_config.yml:/root/config.yml sergdelft/hyperion:pipeline-plugins-renamer-0.1.0
```

### Building the Docker image yourself
The included Dockerfile compiles and bundles the plugin. 
To build it, navigate to the repository root and run the following command:

```shell script
docker build . -f pipeline/plugins/renamer/Dockerfile -t hyperion-renamer:latest
```

Once building completes, the plugin can be ran using the following command, 
assuming that the configuration file is located at `renamer_config.yml`:

```shell script
docker run -it -rm -v ${PWD}/renamer_config.yml:/root/config.yml hyperion-renamer:latest
```

## Configuration

This plugin accepts configuration in a YAML file supplied as a command line argument. The following options are accepted:

```yaml
# The list of fields to rename. Fields that are listed here but are not in the
# input are ignored, without throwing an error.
rename:
  -
    # The field that should be renamed. Can be a nested path using periods.
    from: "log_line"
    # The target location of the value. Can be a nested path using periods.
    to: "location.line"
  
  # Other rename entries...
  -
    from: "log_file"
    # etc...

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
    plugin-id: "Renamer"
  
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
    "log_line": 10,
    "log_file": "MyFile.java"
}
```

## Output Format

This plugin will transform the incoming JSON message according to the configuration and output a new JSON object that strictly contains an equal amount of fields as the input, although their structure might be different. For the example given in the usage section, the output is as follows:

```json
{
    "location": {
        "line": 10,
        "file": "MyFile.java"
    }
}
```
