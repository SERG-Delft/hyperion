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
poll-interval: 5

elasticsearch:
  hostname: elk.njkyu.com
  index: logs
  port: 9200
  scheme: http
  timestamp-field: "@timestamp"
  authentication: no
  response-hit-count: 10000

plugin-manager:
  plugin-id: Elasticsearch
  manager-host: "localhost:6379"
``` 

The full spec is as follows:

```yaml
poll-interval: <num>

elasticsearch:
  hostname: <string>
  index: <string>
  port: <num?, default=9200>
  scheme: <string, http | https>
  timestamp-field: <string>
  response-hit_count: <num>
  authentication: <boolean>

  # username and password are necessary
  # if authentication=true
  username: <string?>
  password: <string?>

plugin-manager:
  id: <string>
  host: <string>
  buffer-capacity: <num?, default=20000>
``` 
