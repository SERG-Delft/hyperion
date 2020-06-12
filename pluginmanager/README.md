# Hyperion - Plugin Manager

![Gradle check PluginManager](https://github.com/SERG-Delft/hyperion/workflows/Gradle%20check%20PluginManager/badge.svg)

This project is the core router for the Hyperion pipeline. The order of plugins is configured here, after which it will ensure that all plugins can run standalone and query this single plugin manager for their location in the pipeline and how to communicate with their previous and next counterparts.

For more information on the Hyperion and plugin manager protocol, please see [The Hyperion pipeline protocol in more detail](/docs/protocol.md).

## Building & Running

To build the library, run `gradle pluginmanager:shadowJar`. The result will be located in `build/pluginmanager-all.jar`.

To execute the tests and linting, run `gradle pluginmanager:check`.

To run a compiled version of the plugin manager, simply launch it using Java:

```shell script
java -jar build/pluginmanager-all.jar [path to config]
```

## Docker

If you have a built version of this plugin, you can use the accompanied Dockerfile to set up an image with all pre-requisites installed. Please note that this Dockerfile will not compile the plugin for you.

Please note that the docker container for this plugin will load the configuration file from the `CONFIGFILE` environment variable, and not the command line arguments.

## Configuration

The plugin manager accepts configuration in a YAML file supplied as a command line argument. The following options are accepted:

```yaml
# The host:port pair to bind the plugin manager against. This URL must be accessible
# by all plugins in the pipeline, such that they can talk with the plugin manager.
# Use tcp://*:port or tcp://0.0.0.0:port to bind on all interfaces.
host: "tcp://localhost:5560"

# The list of plugins in the pipeline. Data traverses through the pipeline in the
# order specified in this list. Note that the first element must be a plugin capable
# of supplying data (a data source) while the last plugin will almost always be the
# aggregator.
#
# Note that while a plugin does not need to be able to talk to all other plugins, it
# needs to be able to talk to the plugin before it and the plugin after it. It is
# therefore recommended that all the plugin machines run on the same (virtual) network.
# 
# Note that a plugin can either connect or bind on a port. The way the plugin manager
# works is that the data source binds. Then, plugin 1 connects to the data source and
# binds a server for the next plugin. Plugin 2 will connect to plugin 1, and bind a new
# server for plugin 3, etc. That means that the host/port pair as specified in the list
# means that the host that runs that specific plugin must have that port free so that
# the plugin is able to bind to that port. This also means that the last plugin host
# effectively does not matter, as it will only connect to the previous plugin and never
# bind a new port on its own.
plugins:
  -
    # The ID of this plugin stage. This must match the ID as configured in the
    # pipeline.plugin-id configuration of the plugin in question, and must be
    # unique across all steps.
    id: "Datasource"
    # The host that this plugin will bind to. The next plugin must be able to 
    # connect to this host. See the notes at the top of the plugin section for
    # more information. Please note that the wildcard host cannot be used, as
    # the next plugin must know the hostname of this plugin.
    host: "tcp://localhost:1200"
  
  # Other steps follow...
  -
    id: "Renamer"
    host: "tcp://localhost:1201"

  # ...

  # Finally, ends on the aggregator
  - id: "Aggregator"
    host: "tcp://localhost:1202"
```
