# Simple Elasticsearch pipeline
This simple pipeline polls data from [Elasticsearch](https://www.elastic.co/), runs a pipeline plugin and aggregates the log data.

To run this pipeline you need an [Elasticsearch](https://www.elastic.co/) server containing log data and
a machine with the [docker-engine](https://docs.docker.com/engine/) 
and [docker-compose](https://docs.docker.com/compose/) installed on it.
To view the aggregated data you can either poll postgres directly, use a web browser or use a
Hyperion compatible front-end plugin. Hyperion ships with a plugin for
[Intellij IDEA](https://www.jetbrains.com/idea/) which can be used with this pipeline.
Execute the steps below to run the pipeline.
 

## 1. Configure the datasource
Change the `hostname` and `port` fields in `datasource.yml` to point to your server. 
It might be necessary to set authentication and headers for your setup as well. 
For a complete overview of available configuration options, visit the readme of the elasticsearch datasource plugin.

example setup:
```yaml
elasticsearch:
  # replace hostname with your own elasticsearch instance
  hostname: elk.njkyu.com
  port: 9200
  scheme: http
  authentication: no
```

## 2. Build all jars using docker
```shell script
$ cd src/systemTest/elasticsearch-pipeline/
$ docker-compose -f docker-compose.build.yml up
```

Notes:
- The current builder does not cache dependencies
- It mounts the host project in the container to build

## 3. Run the pipeline
```shell script
$ docker-compose up -d
``` 

## Inspecting results
In order to see the aggregated data visit `http://localhost:8081/api/v1/metrics?project=TestProject&file=path.path.path...TestFile&intervals=10,20,30` in a webbrowser on the machine.
Check out how to setup the Intellij IDEA plugin for showing results at plugin/src/main/README.md