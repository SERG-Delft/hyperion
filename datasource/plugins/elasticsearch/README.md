# Hyperion - ElasticSearch Data Source Plugin

![Gradle check elasticsearch plugin](https://github.com/SERG-Delft/hyperion/workflows/Gradle%20check%20elasticsearch%20plugin/badge.svg)

> **Important**: If you want to integrate Hyperion with the ELK stack, consider using [logstash-output-hyperion](/logstash-output-hyperion) instead. It is much more performant than the ElasticSearch plugin and designed specifically for high throughput.

Plugin that acts as an adapter between an ElasticSearch instance and Hyperion. It works by continuously pulling the ElasticSearch instance for new data and pushing it to the next stages of the pipeline.

## Building & Running

To build the data source, run `gradle datasource:elasticsearch:shadowJar`. The result will be located in `build/elasticsearch-all.jar`.

To execute the tests and linting, run `gradle datasource:elasticsearch:check`.

To run a compiled version of the aggregator, simply launch it using Java:

```
$ java -jar elasticsearch-all.jar                                                     
Usage: command [OPTIONS] COMMAND [ARGS]...

  Periodically pull data from an Elasticsearch instance and send it to the
  Hyperion pipeline.

Options:
  -h, --help  Show this message and exit

Commands:
  verify  Verify if the config file is in the correct format
  run     Run with the the given config file

$ java -jar elasticsearch-all.jar run datasource-es.yml
[main] INFO nl.tudelft.hyperion.datasource.plugins.elasticsearch.Elasticsearch - Elasticsearch client created successfully
[main] INFO nl.tudelft.hyperion.datasource.plugins.elasticsearch.Elasticsearch - Starting retrieval of logs
...
```

## Docker

If you have a built version of this plugin, you can use the accompanied Dockerfile to set up an image with all pre-requisites installed. Please note that this Dockerfile will not compile the plugin for you.

## Configuration

The data source expects a configuration YAML file, given as a command line argument. 

The following configuration options are accepted:

```yaml
# The amount of time between consecutive polling intervals in seconds. All new
# logs that arrived will be retrieved once, after which the data source will
# sleep for the specified amount of seconds.
poll-interval: 5

# Configuration options for the ElasticSearch instance.
elasticsearch:
    # The hostname at which the ElasticSearch instance is located.
    hostname: elk.njkyu.com

    # The port on which the ElasticSearch instance is running.
    port: 9200

    # The index(es) from which logs should be pulled. This uses the same
    # ElasticSearch index format as the raw ES API, and is documented at
    # https://www.elastic.co/guide/en/elasticsearch/reference/current/multi-index.html
    index: logs
  
    # The scheme to be used for connecting to the ES instance. Either http or https.
    scheme: http
  
    # The field to be used as the timestamp field. This field will be used
    # to determine whether a log entry has already been processed or not.
    timestamp-field: "@timestamp"

    # Whether to use authentication for talking to ES. 
    authentication: true
  
    # The username to use for connecting to ES. Required if `authentication = true`.
    username: myusername

    # The password to use for connecting to ES. Required if `authentication = true`.
    password: mypassword

    # The maximum amount of logs to be queried in a single `poll-interval`.
    # Note that while this value can be larger than 10k, ES will only return
    # up to 10,000 entries at once.
    response-hit-count: 10000

# Various settings needed for the data source to interact with the pipeline,
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
    plugin-id: "ElasticSearch"
  
    # The size of the internal buffer used for storing data that has not been sent in
    # the 0MQ pipeline. Increasing this will allow for more messages to be buffered,
    # at the cost of more memory usage. Defaults to 20k.
    buffer-size: 20000
``` 

## Output Format

The ES data source will output the raw fields that are stored in ElasticSearch as a JSON document. While the exact format will depend on how you configured your ElasticSearch, it will likely look somewhat like this if using a "standard" ELK stack:

```json
{
    "@version": "1",
    "message": "[04 30 11:33:32] INFO com.sap.enterprises.server.impl.TransportationService:37 - Move service successful",
    "received_at": "2020-05-01T08:32:18.324Z",
    "log4j_file": "com.sap.enterprises.server.impl.TransportationService",
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
