# Elasticsearch DataSource Plugin

Plugin that acts as an adapter between an Elasticsearch instance and Hyperion.  

### Example usage

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

### Example `datasource-es.yml`

```yaml
poll_interval: 5
id: Elasticsearch

elasticsearch:
  hostname: elk.njkyu.com
  index: logs
  port: 9200
  scheme: http
  timestamp_field: "@timestamp"
  authentication: no
  response_hit_count: 10000

zmq:
  host: localhost
  port: 6379
``` 

The full spec is as follows:

```yaml
poll_interval: <num>
id: <string>

elasticsearch:
  hostname: <string>
  index: <string>
  port: <num?, default=9200>
  scheme: <string, http | https>
  timestamp_field: <string>
  response_hit_count: <num>
  authentication: <boolean>

  # username and password are necessary
  # if authentication=true
  username: <string?>
  password: <string?>

zmq:
  host: <string>
  port: <num>
  buffer_capacity: <num?, default=20000>
``` 
