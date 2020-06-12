# Hyperion - Path Extractor Plugin

![Gradle check pipeline pathextractor plugin](https://github.com/SERG-Delft/hyperion/workflows/Gradle%20check%20pipeline%20pathextractor%20plugin/badge.svg)

This package provides a path extractor plugin that is able to rename fields with `java.style.package.Names` into their appropriate `src/main/java/style/package/Names.java` file name. It does this in a dumb manner, simply replacing periods and prefixing a configurable path. If you need a more complex renaming strategy, consider [making your own plugin](/docs/writing-java-kotlin-plugin.md). 

## Usage

_For full details on the supported configuration format, please see the [configuration section](#Configuration) of this document_.

The path extractor plugin is very simple and only has two relevant fields:

```yaml
field: "log4j_file"
relative-source-path: "src/main/java"
postfix: ".java"
```

As should already be clear from the naming, in this configuration the plugin will convert an incoming message like

```json
{
  "log4j_file": "com.foo.Bar"
}
```

into

```json
{
  "log4j_file": "src/main/java/com/foo/Bar.java"
}
```

> *Note*: If you use Kotlin, this plugin will automatically take care of removing any Kt suffixes added.

## Building & Running

To build the library, run `gradle pipeline:pathextractor:shadowJar`. The result will be located in `build/pathextractor-all.jar`.

To execute the tests and linting, run `gradle pipeline:pathextractor:check`.

To run a compiled version of the path extractor plugin, simply launch it using Java:

```shell script
java -jar build/pathextractor-all.jar [path to config]
```

## Docker

If you have a built version of this plugin, you can use the accompanied Dockerfile to set up an image with all pre-requisites installed. Please note that this Dockerfile will not compile the plugin for you.

Please note that the docker container for this plugin will load the configuration file from the `CONFIGFILE` environment variable, and not the command line arguments.

## Configuration

This plugin accepts configuration in a YAML file supplied as a command line argument. The following options are accepted:

```yaml
# The field that contains a Java package and should be transformed into
# a field name. This may be a nested path using periods.
field: "log4j_file"

# The base relative path that should be prepended to the transformed package
# name. If using Gradle or Maven, this is usually src/main/java or src/main/kotlin.
relative-source-path: "src/main/java"

# The postfix to be added to the log file. Usually `.java` or `.kt`.
postfix: ".java"

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
    plugin-id: "PathExtractor"
  
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
    "log4j_file": "com.foo.Bar"
}
```

## Output Format

This plugin will transform the incoming JSON message according to the configuration and output a new JSON object that strictly contains an equal amount of fields as the input. For the example given in the usage section, the output is as follows:

```json
{
    "log4j_file": "src/main/java/com/foo/Bar.java"
}
```
