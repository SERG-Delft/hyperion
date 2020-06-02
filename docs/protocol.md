# The Hyperion pipeline protocol in more detail
The Hyperion pipeline consists of two main parts; plugins and the plugin-manager.
The pipeline is an ordered sequence of plugins meaning a message will only be processed once by a specific instance of a plugin.
The order of plugins through which messages are passed is defined in the configuration of the plugin-manager.
Each pipeline contains a datasource plugin as its starting point, it will only push messages through the pipeline.
Similarly, each pipeline uses the Aggregator as its end point, it will only pull messages from the pipeline.
In between the datasource and Aggregator zero to many pipeline-plugins can exist.
Each pipeline-plugin pulls a message from the previous plugin, processes it in the way it want and pushes the product to the successive plugin.

## Protocol plugin <-> plugin-manager
Before a plugin can start processing messages it needs to know two things;
Where do I retrieve messages (i.e. pull) from?
Where do I send (i.e. push) processed messages to? 
Since the configuration file of the plugin-manager states in which order plugins should handle messages,
a plugin should retrieve this information from the plugin-manager.
The plugin-manager communicates with the plugins in a `request/reply`  fashion using `ZMQ` sockets.
The plugin-manager will bind to the `host` field specified in its configuration while plugins will `connect` to the same port.
Since requests to the plugin-manager aren't cached it is advisable to start the plugin-manager before running any plugins.
###### Launching a plugin before the plugin-manager implies `connect` will be called before `host`, `ZMQ` can handle this, do note that the plugin will probably never receive its configuration this way since requests aren't cached by the plugin-manager.
A pipeline plugin will make two requests to the plugin-manager, one to receive pull and one to receive push configuration.
The datasource will only query for push configuration, the Aggregator will only request pull configuration.
A configuration request should be formatted as JSON and contain the fields `id` and `type`. where:
- `id` specifies the id of the pipeline-plugin as configured in its configuration.
- `type`is either `"push"` or `"pull"`.

A valid configuration request looks like:
```json
{"id": "renamer", "type": "pull"}
```
The plugin-manager will lookup whether the id `renamer` is specified in its configuration, if it is not it will return `Invalid Request`.
If the id is valid, the plugin-manager will reply with configuration details, in this case where renamer should pull from:
```json
{"isBind": "false", "host": "tcp://localhost:30000"}
```
After these messages no further communication is expected by either party.
![Sequence diagramg for protocol between Plugin and PluginManager](https://i.imgur.com/UspJSu0.jpg)

## Protocol between pipeline plugins
Plugins push messages from after processing to the next plugin using `ZMQ` PUSH sockets.
Retrieval of previous messages is handled by `ZMQ` PULL sockets.
Each message is just a string, in theory any string can be pushed or pulled by your pipeline.
The shipped aggregator however requires a specific [JSON format](../aggregator/README.md#Input Format) to work out of the box.
