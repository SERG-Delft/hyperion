# The Hyperion pipeline protocol in more detail

> This document outlines the technical protocol behind the Hyperion pipeline and is mainly relevant if you want to interact with the pipeline from a different language than Kotlin/Java. If you want to create a plugin in Java/Kotlin, consider reading [Writing a new pipeline plugin in Java/Kotlin](/docs/writing-java-kotlin-plugin.md).

> The Hyperion pipeline exclusively uses [ZeroMQ](https://zeromq.org/) for communication between components. It is recommended that the reader has a basic understanding of ZeroMQ before reading this document.

The Hyperion pipeline consists of two main parts: **the plugin manager** and the **plugins**. As the name suggests, the plugin manager is responsible for orchestrating the order in which plugins handle messages and for telling the plugins where to receive and send their data. Crucially however, the plugin manager does not route any messages itself. All message routing is done directly between plugins through ZMQ and the plugin manager is only responsible for communicating network addresses to the plugins.

The following properties hold within the Hyperion pipeline:
- It is an ordered sequence of plugins. A message will be processed by a specific plugin instance exactly _once_. 
- The order of plugins is defined solely in the plugin manager.
- Each pipeline must start with a producer (usually a data source), which supports sending without receiving.
- Each pipeline must end with a consumer (usually the aggregator), which supports receiving without sending.
- All other plugins must support both receiving incoming events and sending outgoing transformed events.
- Transforming/intermediate plugins must only transform messages. They must not produce new messages from thin air.
- Intermediate plugins _may_ drop messages if appropriate (such as when the message cannot be processed, or when internal buffers are full).
- Messages sent between plugins consist of raw text. However, all current plugins assume that the content is JSON.

## Plugin Manager Protocol

Before a plugin can start processing messages it needs to know two things:
- Where do I retrieve messages from (if applicable)?
- Where do I send processed messages to (if applicable)?

To retrieve this information, the plugin can query the plugin manager. It is assumed that the plugin already knows where this manager is located, and that it has some unique ID it can use to identify itself with the plugin manager.

To query this information, the `REQ`/`REP` ZMQ socket pair is used. The plugin will initiate a `REQ` socket, connecting to the plugin manager. It will then query for information using a JSON-based protocol. The plugin manager will use a `REP` socket, bound to a configured port, to respond to these queries. Due to this, the plugin manager needs to be running before a plugin starts.

The following protocol is used for querying connection information. `C` implies client (i.e. the plugin) while `S` implies server (i.e. the plugin manager).

**C:**
```json
{
    "id": "<the unique ID of the plugin, as specified in the plugin configuration (or otherwise provided by the plugin)>",
    "type": "<either push or pull>"
}
```

Example:
```json
{
    "id": "Aggregator",
    "type": "pull"
}
```

If `type` is `"pull"`, the plugin manager will respond with the connection information for the previous plugin in the pipeline (such that the plugin is able to "pull" those messages).

If `type` is `"push"`, the plugin manager will respond with the connection information for the next plugin in the pipeline (such that the plugin is able to "push" messages).

If a different payload is sent, or if the plugin is not known to the plugin manager, it will respond with `Invalid Request`.

---

**S:**
```json
{
    "host": "<either the fully qualified tcp host, or null if there isn't such a step in the pipeline>",
    "isBind": true/false
}
```

Example:
```json
{
    "host": "tcp://123.123.123.123:12345",
    "isBind": true
}
```

`isBind` specifies whether or not the plugin should bind to the given host (i.e. create a server). If `true`, the plugin should bind. If `false`, the plugin should connect.

`host` specifies the host of the neighbor this plugin should connect to. If host is null, it means that there's no such plugin in the queried direction. For example, the plugin manager will return `null` if a plugin queries for `"push"` while it is the last entry in the pipeline. A plugin should use this information to determine whether it is able to receive messages, send messages, or both.

---

After these messages, no further communication is expected from either party. A plugin _may_ query for connection details again, but this is not a requirement. Pipeline changes are not actively pushed to plugins, instead assuming that a pipeline change involves restarting the entire Hyperion pipeline.

The following sequence diagram visualizes the plugin manager protocol:

![Sequence diagram for protocol between plugin and plugin manager](https://i.imgur.com/UspJSu0.jpg)

## Plugin Protocol

Once a plugin has queried the plugin manager for the connection information needed to connect to its _peers_, it will directly use ZMQ `PUSH`/`PULL` sockets to connect to them.

A plugin may either only send messages (a data source), only receive messages (a consumer), or both. A plugin should dynamically query whether or not it can send or receive by asking the plugin manager. Any peer whose `host` is null is not present. A plugin should verify whether it has the necessary connections available (i.e. a plugin should error if it requires the ability to send, but is the last plugin in the pipeline).

To receive incoming messages, a plugin will create a new ZMQ `PULL` socket that either binds or connects to the host provided by the plugin manager, as per the `isBind` variable returned by the plugin manager.

To send outgoing messages, a plugin will create a new ZMQ `PUSH` socket that either binds or connects to the host provided by the plugin manager, as per the `isBind` variable returned by the plugin manager.

The protocol used between plugins is text-based. There is no official codec for the messages, but all current plugins use a JSON-based codec (the aggregator expects a specific [JSON format](/aggregator/README.md#input-format) for example). It is recommended that plugins use a JSON codec, such that they can interact with other (community made) plugins. Only if there is a valid reason for not using a JSON codec, can a plugin use a different format. In such cases, it is recommended to also create a plugin that is able to convert from/to this codec from the JSON codec.
