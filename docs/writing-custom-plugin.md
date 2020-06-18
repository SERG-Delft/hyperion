# Writing a new pipeline plugin in a different language

The Hyperion pipeline was specifically designed such that plugins can be written in any language that can interface with [ZeroMQ](https://zeromq.org) sockets. The default plugins included with Hyperion are written in Kotlin and run on the JVM. If possible, it is recommended to write your custom plugins in a language targeting the JVM too, as this will allow you to use the libraries written and tested by the Hyperion team. For more information, see [Writing a new pipeline plugin in Java/Kotlin](/docs/writing-java-kotlin-plugin.md).

However, to show how simple writing a plugin really is, this tutorial will show how to create a pipeline plugin in a different language. Before you start, it is urged that you read [The Hyperion pipeline protocol in more detail](/docs/protocol.md), as it describes the protocol you need to implement in your plugin in order to talk with the Hyperion pipeline. This tutorial also assumes that you know how to set up an Hyperion pipeline. See [Installing and configuring your first Hyperion pipeline](/docs/hyperion-setup.md) for more info.

For the sakes of this tutorial, we will be writing a simple pipeline plugin in JavaScript running on [Node.js](https://nodejs.org). The plugin will parse the incoming text as JSON, uppercase a configurable field, then send it onwards to the next plugin. We believe that the information in the protocol documentation, alongside this tutorial, should be enough to generalize these instructions to writing a plugin in a different language.

## Setup

Before we can begin, we need to set up a Node project. Ensure that you've [installed Node](https://nodejs.org/download) and that it is available in your path. Then, create a new directory and run `npm init`:

```shell script
$ mkdir my-pipeline-plugin
$ cd my-pipeline-plugin
$ npm init
```

You can answer whatever you like to the questions posed by `npm`.

For this tutorial, we will be using the 6.0 beta release of [zeromq.js](https://github.com/zeromq/zeromq.js), the official Node library for ZeroMQ. Within the plugin folder, run the following command to install the ZeroMQ libraries:

```shell script
$ npm install zeromq@6.0.0-beta.6
```

## Communicating with the plugin manager

Our first step is to ask the plugin manager where we are in the pipeline. We will need this information so that we can connect to our "neighbors" and receive/send messages from/to them. As per the [plugin manager protocol documentation](/docs/protocol.md#plugin-manager-protocol), we can do this by sending a specifically formatted JSON message through a `REQ` ZMQ socket.

We can reference the [general ZMQ documentation](https://zeromq.org/get-started), as well as the [zeromq.js documentation](https://github.com/zeromq/zeromq.js) on how to do this in Node.

Start by putting the following code in `index.js`:

```javascript
const zmq = require("zeromq");

// Connect to the plugin manager located at `managerHost`, identify
// ourselves as `id` and request connection information. Returns an
// object of { pull: <sub info>, push: <pub info> }.
async function retrieveConnectionDetails(id, managerHost) {
    const sock = new zmq.Request();
    await sock.connect(managerHost); // connect to plugin manager

    // Request pull info.
    await sock.send(JSON.stringify({ id, type: "pull" }));
    const [pull] = await sock.receive();

    // Request push info.
    await sock.send(JSON.stringify({ id, type: "push" }));
    const [push] = await sock.receive();

    return {
        pull: JSON.parse(pull),
        push: JSON.parse(push)
    };
}
```

While this code lacks some important things that production code should really have (such as logging and error handling), it should be enough to show the general concept. We connect to the plugin manager and request information on where to `pull` messages from, and where to `push` messages to.

We can test whether this works by adding the following to the file, then running `node index.js`. Note that you will need to have a plugin manager running on port 3000 (or modify the code accordingly). If your plugin manager isn't running, this code will hang endlessly until it is able to connect to the plugin manager.

```javascript
(async() => {
    const details = await retrieveConnectionDetails("MyPipelinePlugin", "tcp://localhost:3000");

    console.log("I'll need to pull data from:", details.pull);
    console.log("I'll need to push data to:", details.push);
})();
```

## Sending and receiving data

Now that we can query the plugin manager for connection details, we can use those details to connect to our adjacent plugins and start sending and receiving data! Due to the simplicity of the pipeline protocol, this requires only very few lines. For more information, please see the [protocol documentation](/docs/protocol.md).

Add the following to your `index.js`:

```javascript
const FIELD = "message"; // which field to uppercase

// Helper that connects/binds sock to the connection
// details in `details`, as sent by the plugin manager.
async function connectSocket(sock, details) {
    if (details.isBind) {
        await sock.bind(details.host);
    } else {
        await sock.connect(details.host);
    }
}

// Endlessly pulls messages from the previous plugin,
// transforms them and pushes them to the next one.
async function runPullPushLoop(connectionDetails) {
    const pull = new zmq.Pull();
    await connectSocket(pull, connectionDetails.pull);

    const push = new zmq.Push();
    await connectSocket(push, connectionDetails.push);

    // For every incoming message.
    for await (const [msg] of pull) {
        // Parse...
        const obj = JSON.parse(msg);

        // transform...
        if (obj && obj[FIELD]) {
            obj[FIELD] = obj[FIELD].toUpperCase();
        }

        // and push
        await push.send(JSON.stringify(obj));
    }
}
```

This code should be fairly straightforward. We use the connection details from the plugin manager to connect to both the previous plugin and the next plugin (invoking either `bind` or `connect` based on what the plugin manager tells us). After that, we asynchronously receive messages from the `pull` socket, parse them as JSON, uppercase the configured field, and finally send the result to the next plugin.

## Tying it all together

Now that we can pull connection information and we can transform messages, we just need to tie it all together. Simply add the following on the bottom of your function, replacing the old plugin manager testing code if you had it:

```javascript
// ...
// retrieveConnectionDetails, connectSocket, runPullPushLoop are here
// ...

(async() => {
    const details = await retrieveConnectionDetails("MyPipelinePlugin", "tcp://localhost:3000");

    console.log("Starting my pipeline plugin...");
    await runPullPushLoop(details);

    // The loop is infinite, so we should never reach this.
})();
```

That's it! Running `node index.js` will start your plugin, connect to the plugin manager, and finally connect to the other stages in the pipeline. Note that while this is a working plugin, it is recommended that you add error handling and logging to make it more robust. You can use a tool such as [pm2](https://pm2.keymetrics.io/) to then run the plugin in the background.

You can see the final version of `index.js` [here](https://gist.github.com/molenzwiebel/0a46876665b99b2d7fde80b8ed0c262b).

## Extra: Testing Your Plugin

If you want to test your plugin as part of a real pipeline, there's a pair of plugins that are specifically designed for this purpose. The [reader](/pipeline/plugins/reader) plugin reads input from stdin and sends it into the pipeline, while the [printer](/pipeline/plugins/printer) plugin prints any messages it receives. As such, if you want to test your plugin in a real pipeline, you can set up a plugin manager that uses a reader as input, routes it to your plugin, then routes it to the printer plugin.