# Hyperion - Extractor Plugin

TODO: Status badge

This package provides an extractor pipeline plugin that is able to execute a regular expression on a field and extract the results to separate fields. It can be used to extract values from a single log line.

## Usage

_For full details on the supported configuration format, please see the [configuration section](#Configuration) of this document_.

The extractor plugin works by parsing incoming messages and executing a regular expression on a specified field. The capture groups from the regular expression can then optionally be converted and written to a new JSON field.

For example, given the following (partial) configuration:

```yaml
fields:
  - field: "message"
    match: "\\[.+?\\] INFO [^:]+:(\\d+) - (.+)"
    extract:
      - to: "location.line"
        type: "number"
      - to: "log_message"
        type: "string"
```

The regular expression given matches text that looks like `[Apr 10] INFO com.foo.Bar:10 - Message`, and will capture the line and message respectively.

If this plugin then receives the following as input:

```json
{
    "message": "[Apr 10] INFO com.foo.Bar:10 - Message"
}
```

It will extract and convert accordingly, resulting in the following:

```json
{
    "message": "[Apr 10] INFO com.foo.Bar:10 - Message",
    "log_message": "Message",
    "location": {
        "line": 10
    }
}
```

From this result, it should be obvious that this plugin simply uses the captures generated by the regex configured in `match`, potentially converts them to a different format, and finally writes them to the specified output field. The original input field is left untouched.

## Building & Running

To build the library, run `gradle pipeline:extractor:shadowJar`. The result will be located in `build/extractor-all.jar`.

To execute the tests and linting, run `gradle pipeline:extractor:check`.

To run a compiled version of the extractor plugin, simply launch it using Java:

```shell script
java -jar build/extractor-all.jar [path to config]
```

## Docker

If you have a built version of this plugin, you can use the accompanied Dockerfile to set up an image with all pre-requisites installed. Please note that this Dockerfile will not compile the plugin for you.

Please note that the docker container for this plugin will load the configuration file from the `CONFIGFILE` environment variable, and not the command line arguments.

## Configuration

This plugin accepts configuration in a YAML file supplied as a command line argument. The following options are accepted:

```yaml
# A list of extraction patterns that need to be applied to incoming messages. If
# a field is not present in a message, it is skipped in the processing. You can have
# any number of these field extractions configured.
fields:
  -
    # The name of the field to execute the regular expression on.
    field: "message"
    # The regular expression to execute on the field. Please note that
    # you will need to escape backslashes as you would if this was a 
    # normal string literal.
    match: "\\[.+?\\] INFO [^:]+:(\\d+) - (.+)"
    # The list of targets for the matched groups in the regex. This is
    # processed in the same order of the groups as they appear in the
    # regular expression. In case there is a mismatch in size between
    # the groups matched and the extract list, the smaller of the two
    # is chosen.
    extract:
      -
        # The target field for this extraction. This can be a nested
        # expression delimited by dots, in which case it will automatically
        # create child objects as needed.
        to: "location.line"
        # The type of the value to write to the field. The extractor plugin
        # supports converting the extracted string value from the regular 
        # expression to a different format. The following types are supported:
        # "string", "number", "double"
        type: "number"
      
      # Add extract entries as necessary...
      -
        to: "message_text"
        type: "string"
    
  # Add more fields as necessary
  -
    field: "other_message"
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
    plugin-id: "MyPlugin"
  
    # The size of the internal buffer used for storing data that has not yet been processed
    # locally. Increasing this will allow for more messages to be buffered, at the cost of
    # more memory usage. Messages incoming while the buffer is full will be thrown away. If
    # this happens often, consider using the load balancer plugin to shard this plugin across
    # multiple instances.
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

This plugin will transform the incoming JSON message according to the configuration and output a new JSON object that strictly contains more values than the input. For the example given in the usage section, the output is as follows:

```json
{
    "message": "[Apr 10] INFO com.foo.Bar:10 - Message",
    "log_message": "Message",
    "location": {
        "line": 10
    }
}
```