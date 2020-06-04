# Example pipeline configurations for common devops setups

> This document assumes familiarity with the Hyperion basics. If you haven't already, please read [Installing and configuring your first Hyperion pipeline](/docs/hyperion-setup.md).

The Hyperion pipeline is all about flexibility and composability. We have created a set of default plugins that each do elementary transformations on your input values. By chaining these plugins together, you should be able to transform your logs into a format that the aggregator accepts, regardless of what format it was originally in.

As a reminder, at the end of the pipeline your data needs to adhere to the following format. Extra fields are ignored. For more information, see the [aggregator](/aggregator) documentation.

```json
{
    "project": "<some unique identifier for the project, such as the repo name or package>",
    "version": "<some way to represent the version the code is running on, usually a git hash>",
    "location": {
        "file": "<the file in which this log entry occured, relative to the root of the code>",
        "line": "<the line in which the log happened, can be a string or a number>"
    },
    "severity": "<some way to represent the severity, recommended to be a standard severity but free form>",
    "timestamp": "<an ISO 8601-parsable timestamp that represents the time at which this log occurred>"
}
```

As such, you will need to engineer your pipeline in a way that the plugins transform your input format into this final format. This document has a list of commonly used plugins and example scenarios in which they can be used.

If you need a way to do something not listed here, we've made it easy to create custom plugins! See our documentation on [creating a new pipeline plugin in Java/Kotlin](/docs/writing-java-kotlin-plugin.md) or [creating a new pipeline plugin in a different language](/docs/writing-custom-plugin.md).

## Document structure

This document has two sections. First, there is a list of plugins and example scenarios in which they may be used. These each describe the plugins workings on a simple level. The [plugin index](#plugin-index) has a full list of the plugins and the scenarios in which they may be used.

Secondly, the (example pipelines)[#example-pipelines] section contains a set of example input payloads and the recommended set of plugins that can be used to transform them to the required output format. They are meant to be abridged versions of the [main Hyperion pipeline tutorial](/docs/hyperion-setup.md), and as such do not detail full configuration files but rather a list of plugins and references to their appropriate documentation.

## Plugin Index

I want to...

- [Inspect the current messages going through the pipeline](#plugin-printer)
- [Measure the rate of messages going through the pipeline](#plugin-rate)
- [Rename or move a field](#plugin-renamer)
- [Add a statically known value to the payload](#plugin-adder)
- [Extract string values from a field into their own fields](#plugin-extractor)
- [Transform a Java package name to a file path](#plugin-pathextractor)
- [Attach a version tag based on the latest commit in a Git repository](#plugin-versiontracker)
- [Load balance multiple instances of a single plugin to increase pipeline throughput](#plugin-loadbalancer)

Want to do something not listed here? You might be able to do it by combining multiple plugins. If that doesn't work, check out our documentation on [creating a new pipeline plugin in Java/Kotlin](/docs/writing-java-kotlin-plugin.md) and [creating a new pipeline plugin in a different language](/docs/writing-custom-plugin.md).

### Plugin: Printer

The [printer](/pipeline/plugins/printer) plugin is a useful tool for anyone that wishes to debug the messages going through their pipeline. As the name suggests, it simply prints any incoming messages to stdout before forwarding it to the next plugin. Use it to get an idea of the type of data flowing through your pipeline.

Due to its simplicity, the printer plugin has no additional configuration. If needed though, you can find the documentation [here](/pipeline/plugins/printer/README.md).

### Plugin: Rate

The [rate](/pipeline/plugins/rate) plugin is useful for getting an idea of how many messages are flowing through the Hyperion pipeline. Just like the printer plugin, it is mainly intended for debugging and benchmarking the Hyperion stack.

The rate plugin has a single configuration option:

```yaml
rate: 10
```

The `rate` option controls how often the plugin prints a summary of the amount of messages that passed through it. All messages received will additionally be forwarded to the next plugin.

For more information and the full set of configuration options, please see the [rate documentation](/pipeline/plugins/rate).

### Plugin: Renamer

If you want to rename or move a field, the [renamer](/pipeline/plugins/renamer) plugin is your best friend. It has one job: rename (nested) fields to other (nested) fields.

Given a configuration of

```yaml
rename:
  - from: "log_line"
    to: "location.line"
```

It will transform an input of

```json
{
    "log_line": "Foo"
}
```

into

```json
{
    "location": {
        "line": "Foo"
    }
}
```

For more information and the full set of configuration options, please see the [renamer documentation](/pipeline/plugins/renamer).

### Plugin: Adder

If you want to add a statically known value to incoming messages, the [adder](/pipeline/plugins/adder) plugin is the right tool for the job. It will add configurable values to all incoming messages, only overwriting fields that do not have a value already.

With a configuration that contains

```yaml
add:
  - key: "animals.chicken"
    value: "egg"
```

It will transform an input of

```json
{
    "log_line": "Foo"
}
```

into

```json
{
    "log_line": "Foo",
    "animals": {
        "chicken": "egg"
    }
}
```

For more information and the full set of configuration options, please see the [adder documentation](/pipeline/plugins/adder).

### Plugin: Extractor

If you want to extract string values from a field into their own fields, consider using the [extractor](/pipeline/plugins/extractor) plugin. It is similar to the Logstash [grok](https://www.elastic.co/guide/en/logstash/current/plugins-filters-grok.html) or [dissect](https://www.elastic.co/guide/en/logstash/current/plugins-filters-dissect.html) plugins.

The extractor plugin works by executing a regular expression on a specified field, then extracting the matched groups in their own fields based on the configuration.

As an example, assume that your logs follow the following pattern and that we want to extract the package, severity, line and message:

```
[Apr 10] INFO com.foo.Bar:10 - Message
```

One can use the following extractor configuration to do so. Note that the line is additionally converted to a number:

```yaml
fields:
  - field: "message"
    match: "\\[.+?\\] (\\w+) ([^:]+):(\\d+) - (.+)"
    extract:
      - to: "severity"
      - to: "location.file"
      - to: "location.line"
        type: "number"
      - to: "message"
```

For more information and the full set of configuration options, please see the [extractor documentation](/pipeline/plugins/extractor).

### Plugin: Path Extractor

If your logging setup is only logging Java package names and you want to convert them to the file that contained them, consider using the [path extractor](/pipeline/plugins/pathextractor) plugin. It is specifically designed for transforming package names into files, as this was a core requirement during Hyperion development. Note however that the path extractor plugin is "dumb": it simply does a string translation, assuming that your file hierachy matches the package name. If this is not the case, you might need a custom plugin.

With a configuration that contains

```yaml
field: "log4j_file"
relative-source-path: "src/main/java"
postfix: ".java"
```

It will transform an input of

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

For more information and the full set of configuration options, please see the [path extractor documentation](/pipeline/plugins/pathextractor).

### Plugin: Version Tracker

Do you use Git to store your code? Do you have a specific branch deployed to production at all times? If yes, you can use the [version tracker](/pipeline/plugins/versiontracker) plugin to automatically pull the latest commit hash from the code that is currently running in production.

The version tracker will periodically pull the latest version from your git repository and tag all incoming messages with a matching project. As such, may be off for small periods if the version tracker has yet to pull the new commit hash after a successful deploy.

Only git repositories are supported. Authentication is supported over HTTP, HTTPS and SSH. Setting up a read-only SSH key is recommended.

An example configuration of the version tracker looks like this:

```yaml
projects:
  mock-logging:
    repository: https://github.com/nickyu42/mock-elastic-logging
    branch: refs/heads/master
    update-interval: 60
```

This will attach the hash of the latest commit of `master` to all messages whose `project` field is set to `"mock-logging"`.

For more information and the full set of configuration options, please see the [version tracker documentation](/pipeline/plugins/versiontracker).

### Plugin: Load Balancer

The load balancer is a bit of a special plugin. It provides no functionality on its own, but rather provides infrastructure for running multiple workers in parallel. It does this by hosting a "fake" plugin manager, which the worker plugins connect to. As far as the worker plugins are aware, they're just part of a normal pipeline.

For full documentation on the load balancing plugin, please see the [load balancer documentation](/pipeline/plugins/loadbalancer). Please note that the load balancer is a built-in way to support parallelizing your pipeline, but unless you are dealing with an extreme amount of events or have a very slow custom plugin, you will likely not need it. The built-in plugins are able to scale to tens of thousands of messages a second.

## Example Transformation Pipelines

This section will show some examples of input formats originating from your data source and how to transform them to the expected aggregator format. If you think an example is missing and could be useful to document, pull requests are welcome!

List of current examples:
- [Integrating Hyperion with a LogStash pipeline that already extracts fields](#example-logstash-and-hyperion)
- [Manually extracting metadata from a string field](#example-extracting-from-strings)

### Example: Logstash and Hyperion

Hyperion shines the brightest when you already have a proper LogStash pipeline that is able to separate your fields for you. This way, setting up the Hyperion pipeline is as simple as combining a few of the base [plugins](#plugin-index).

For example, assume that you've already set up your pipeline to extract key information from the log messages (likely by using the [grok](https://www.elastic.co/guide/en/logstash/current/plugins-filters-grok.html) or [dissect](https://www.elastic.co/guide/en/logstash/current/plugins-filters-dissect.html) plugin). Your logstash messages may look something like this:

```json
{
    "@version": "1",
    "message": "[04 30 11:33:32] INFO com.sap.enterprises.server.impl.TransportationService:37 - Move service successful",
    "received_at": "2020-05-01T08:32:18.324Z",
    "log4j_file": "com.sap.enterprises.server.impl.TransportationService",
    "log4j_line": "27",
    "log4j_level": "INFO",
    "log4j_message": "Move service successful",
    "ecs": {
        "version": "1.4.0"
    },
    "log": {
        "file": {
            "path": "/var/log/mock/sap.log"
        },
        "offset": 698007
    },
    "fields": {
        "service": "log4j"
    },
    "tags": [
        "beats_input_codec_plain_applied"
    ],
    "agent": {
        "version": "7.6.2",
        "hostname": "e6bcc5f205c8",
        "type": "filebeat",
        "id": "c7636920-45a1-4855-8819-a8adfeffe154",
        "ephemeral_id": "b3f2836f-0f68-4db5-a0aa-3d8e2b4f43be"
    },
    "host": {
        "name": "e6bcc5f205c8"
    },
    "received_from": "{\"name\":\"e6bcc5f205c8\"}",
    "input": {
        "type": "log"
    },
    "@timestamp": "2020-05-01T08:32:18.324Z"
}
```

Here, earlier transformations in the logstash pipeline have already extracted `log4j_file`, `log4j_line`, `log4j_level` and the appropriate timestamp from the input. Therefore, the only things we need to do to integrate the result with Hyperion is to get the data into the pipeline and shuffle a few fields around.

An example pipeline setup may therefore be:
- [logstash-output-hyperion](/logstash-output-hyperion), for exporting logstash events to Hyperion.
- [renamer](/pipeline/plugins/renamer), for moving `@timestamp`, `log4j_file` and `log4j_line` to `timestamp`, `location.file`. and `location.line` respectively.
- [pathextractor](/pipeline/plugins/pathextractor), for converting the Java package in `location.file` to a file path.
- Either [renamer](/pipeline/plugins/renamer) for moving `fields.service` to `project`, or [adder](/pipeline/plugins/adder) for statically adding a `project` field. This depends on whether you want to pull the service from logstash or have it assigned a constant value.
- Either [renamer](/pipeline/plugins/renamer) or [versiontracker](/pipeline/plugins/versiontracker), depending on whether code version data is stored in logstash or needs to be manually queried.
- [aggregator](/aggregator), for aggregating the final values.

Incidentally, the [main tutorial](/docs/hyperion-setup.md) describes a setup similar to this. For more information, we recommend you check out the tutorial.

### Example: Extracting from strings

Depending on your configuration, you may not be able to run a system such as logstash to pre-extract values from log messages before they enter the pipeline. Luckily however, the Hyperion pipeline supports this.

As an example, assume you're using a logging system such as `log4j` and that you have configured it to use the following pattern:

```
[%d{MMM dd HH:mm:ss}] %p %c:%L - %m%n
```

This pattern outputs lines that look like this:

```
[Mar 30 11:33:32] INFO com.sap.enterprises.server.impl.TransportationService:37 - Move service successful
```

Let's also assume that we have messages coming into the Hyperion pipeline that are simply JSON payloads with a single `message` field, like this:

```json
{
    "message": "[Mar 30 11:33:32] INFO com.sap.enterprises.server.impl.TransportationService:37 - Move service successful"
}
```

Note that since the Hyperion pipeline currently has no unified method for date parsing, we will instead be ignoring the timestamp entirely. This will require setting the `verify-timestamp` setting of the aggregator to `false`.

To process these messages, we can set up the following pipeline:
- Some data source able to provide messages as per the documented format.
- The [extractor](/pipeline/plugins/extractor) plugin, using the following regular expression: `\[.+?\] (.+?) ([^:]+):(\d+) - (.+)`. Note that you may need to escape this expression.
- The [adder](/pipeline/plugins/adder) plugin, for adding a constant `project` field to the payload.
- The [versiontracker](/pipeline/plugins/versiontracker) plugin, for attaching the correct git version to the payload.
- The [aggregator](/aggregator), for aggregating the final values.

The documentation pages for each relevant plugin contain examples on how to set the plugins up individually. The [main tutorial](/docs/hyperion-setup.md) describes how to set up a pipeline as a whole.
