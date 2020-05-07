# Elasticsearch DataSource Plugin

Plugin that acts as an adapter between an Elasticsearch instance and Hyperion.  

Can either be ran standalone or automatically via the plugin manager.  
Running in standalone requires the Redis channel to be specified in the configuration.

### Example usage standalone

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

$ java -jar elasticsearch-all.jar run config.yml
[main] INFO nl.tudelft.hyperion.datasource.plugins.elasticsearch.Elasticsearch - Elasticsearch client created successfully
[main] INFO nl.tudelft.hyperion.datasource.plugins.elasticsearch.Elasticsearch - Starting Redis client
[main] INFO nl.tudelft.hyperion.datasource.plugins.elasticsearch.Elasticsearch - Starting retrieval of logs
...
```

### Example `config.yml`

```yaml
poll_interval: 5
name: elasticsearch

elasticsearch:
  hostname: elk.njkyu.com
  index: logs
  port: 9200
  scheme: http
  timestamp_field: "@timestamp"
  authentication: no
  response_hit_count: 10000

redis:
  host: localhost
  port: 6379
``` 

The full spec is as follows:

```yaml
poll_interval: <num>
name: <string>
registration_channel_postfix: <string?, default="-config">

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

redis:
  host: <string>
  port: <num?, default=6379>
``` 
