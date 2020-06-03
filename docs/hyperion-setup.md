# Installing and configuring your first Hyperion pipeline

This document will show you the core concepts behind the Hyperion pipeline and detail how to set up a simple pipeline that takes your logs from your logging source and aggregate them for later visualization in the IDE.

For this tutorial, we will be using [mock-elastic-logging](https://github.com/nickyu42/mock-elastic-logging/) as the project whose logs we will aggregate. It is a simple Java project that will continuously print various dummy logs. It also contains some docker-compose setups for creating a simple ELK stack that aggregates those logs.

To get started, clone the repository and follow the README to setup the ELK instance and logger. If everything is working, you should be able to open the Kibana interface at http://localhost:5601 and see the dummy logs appearing in the log tab.

Without further ado, let's have a look at how the Hyperion pipeline works and how we can configure one ourselves.

### What's the pipeline?

The entire Hyperion pipeline has a single job: attach enough metadata to the incoming log lines to allow the aggregator to do it's job. Because Hyperion aims to be dynamic enough to work in a large variety of logging setups, the composable pipeline allows for any combination of plugins that resolve or attach this metadata.

If we visualize the pipeline setup used in this tutorial, it'd look something like this:

![](https://i.imgur.com/lRIIpBf.png)

At the end of the pipeline (when the aggregator receives the final information), we expect to have the following information (see the [aggregator documentation](/aggregator/README.md) for more info):

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

As such, our job is to set up the pipeline in a way that this metadata is attached and properly positioned in the correct fields. 

### Step -1: Dependencies

First, you need to make sure that you have compiled versions of all the Hyperion projects. You can either download the compiled jars from the GitHub releases, or you can compile the project yourself.

To compile the project yourself, clone the Hyperion project and run the shadowJar goal. All the jars will be put in the `build` subfolder of their specific project:

```shell script
$ git clone https://github.com/serg-delft/monitoring-aware-ides
$ cd monitoring-aware-ides
$ ./gradlew shadowJar
```

### Step 0: The Plugin Manager

Our first step in configuring a pipeline is to set up the plugin manager. The [plugin manager](/pluginmanager/README.md) is the central component that tells all the pipeline components where they will be getting their data from and where it needs to go. Because it is such a centerpiece, it needs to be accessible by all plugins. Since we're just testing, let's run it on `localhost:3000`.

The plugin manager requires a configuration. You can see the full set of options [here](/pluginmanager/README.md), but for now lets start with the bare basics. Put the following in `pluginmanager.yml`:

```yaml
host: "tcp://localhost:3000"
plugins: []
```

We don't have any steps in our pipeline yet, but we will be adding those as we continue the tutorial. For now, you should be able to run the following command to start the plugin manager:

```shell script
$ java -jar path/to/pluginmanager-all.jar pluginmanager.yml
```

### Step 1: Sourcin' the data

Let's start by first ensuring that the pipeline is able to actually retrieve our data. Since we're using the ELK stack, we actually have two options: using the [elasticsearch data source](/datasource/plugins/elasticsearch) or using the [logstash-output-hyperion](/logstash-output-hyperion) plugin. For the sake of simplicity, we will be using the elasticsearch data source in this tutorial, but for production environments you should be using the Logstash plugin, as it is far more performant.

Assuming that your elasticsearch instance is running on localhost and requires no auth, we can use the following bare configuration for the data source (a full list of options can be found [here](/datasource/plugins/elasticsearch/README.md)). Put the following in `elasticsearch.yml`:

```yaml
poll-interval: 5

elasticsearch:
    hostname: localhost
    port: 9200
    index: logs  
    scheme: http
    timestamp-field: "@timestamp"
    authentication: false
    response-hit-count: 10000

pipeline:
    manager-host: "localhost:3000"
    plugin-id: "ElasticSearch"
```

As you can see, we have configured the pipeline manager host to point to the port 3000 that we configured in step 0. If we were to start this plugin right now though, it'd error. Why? Because the plugin manager doesn't know anything about the `ElasticSearch` plugin yet and therefore can't tell it where the data needs to go. Let's fix that first. Edit your `pluginmanager.yml` such that instead of it having an empty `plugins` section, it instead reads the following:

```yaml
plugins:
  - id: "ElasticSearch"
    host: "tcp://localhost:3001"
```

Make sure to restart your plugin manager, then start the elastic search data source as follows:

```shell script
$ java -jar path/to/elasticsearch-all.jar run elasticsearch.yml
```

If everything goes according to plan, the ElasticSearch data source will start broadcasting log data on port 3001. Now, let's add some plugins.

### Step pre-2: What data are we actually processing?

We're now able to get data into the Hyperion pipeline. As per the introduction, we eventually need to transform it into a format that our aggregator can understand. To do that, it might be a good idea to have a look at what data we currently have, so that we can set up a plan on how to transform it into the format that the aggregator requires.

The ElasticSearch data source simply pushes the documents stored in ES in a standard JSON format. As such, let's have a look at what kind of data is stored in ES. You can inspect the data by either going to Kibana and expanding a log entry, or by looking at the [LogStash configuration directly](https://github.com/nickyu42/mock-elastic-logging/blob/master/logstash.conf).

Below is a screenshot of Kibana, with some of the more interesting fields highlighted:

![](https://i.imgur.com/qhvFObO.png)

From this screenshot, we know that the data produced by our data source will look something like this:

```json
{
    "@timestamp": "2020-06-02T10:03:44.000Z",
    "log4j_file": "com.sap.enterprises.server.impl.TransportationService",
    "log4j_level": "INFO",
    "log4j_line": "11",
    "log4j_message": "transportation service created with id=5.088856004746587",
    // extra fields...
}
```

By creatively using the pipeline plugins, we can transform this data into something that the aggregator is able to accept (as a reminder, the format required by the aggregator was discussed earlier in this document).

### Step 2: Movin' fields around.

Let's start with some simple transformations. As a reminder, we want to end with the following format:

```json
{
    "project": "...",
    "version": "...",
    "location": {
        "file": "...",
        "line": "..."
    },
    "severity": "...",
    "timestamp": "..."
}
```

We already have the `severity`, `timestamp` and `line` fields. They're just in the wrong field. Luckily, we can use the [renamer plugin](/pipeline/plugins/renamer) to move them.

Put the following in `renamer.yml`. The config should be fairly straightforward, but you can reference [the full documentation](/pipeline/plugins/renamer) on details.

```yml
rename:
  - from: "@timestamp"
    to: "timestamp"
  - from: "log4j_level"
    to: "severity"
  - from: "log4j_file"
    to: "location.file"
  - from: "log4j_line"
    to: "location.line"

pipeline:
    manager-host: "localhost:3000"
    plugin-id: "Renamer"
```

As with the previous example, also update your `pluginmanager.yml` to include this new plugin (append it to the list of plugins):

```yaml
  - id: "Renamer"
    host: "tcp://localhost:3002"
```

You should now be able to launch the renamer:

```shell script
java -jar path/to/renamer-all.jar renamer.yml
```

After renaming, our messages now look like this. Closer to the expected format, but we still need to transform a few more fields:

```json
{
    "timestamp": "2020-06-02T10:03:44.000Z",
    "location": {
        "file": "com.sap.enterprises.server.impl.TransportationService",
        "line": "11",
    },
    "severity": "INFO",
    "log4j_message": "transportation service created with id=5.088856004746587",
    // extra fields...
}
```

### Step 3: Fixing the file field

As you may have noticed, our `location.file` field currently contains a Java package name, while the [aggregator documentation](/aggregator/README.md) mentions that it should contain the relative path of the file in which the log occurred.

Luckily, we're using a simple Java project where the package names of a file match with it's location. If you look at the [mock-elastic-logging](https://github.com/nickyu42/mock-elastic-logging) repository, you can see that the `TransportationService.java` file is located at `src/main/java/com/sap/enterprises/server/impl/TransportationService.java`. To transform our current package name to the relevant file path, we therefore only need to replace the periods with slashes, prefix `src/main/java/` and suffix `.java`.

Luckily, there's a default Hyperion plugin called [pathextractor](/pipeline/plugins/pathextractor) that is able to do this for us! Let's use the following config in `pathextractor.yml`:

```yaml
field: "location.file"
relative-source-path: "src/main/java"
postfix: ".java"

pipeline:
    manager-host: "localhost:3000"
    plugin-id: "PathExtractor"
```

As always, ensure that you add the following to the end of your plugin manager config:

```yaml
  - id: "PathExtractor"
    host: "tcp://localhost:3003"
```

Launching the path extractor plugin is the same as what we've previously done:

```shell script
java -jar path/to/pathextractor-all.jar pathextractor.yml
```

After the messages pass through the path extractor plugin, they should now look like this:

```json
{
    "timestamp": "2020-06-02T10:03:44.000Z",
    "location": {
        "file": "src/main/java/com/sap/enterprises/server/impl/TransportationService.java",
        "line": "11",
    },
    "severity": "INFO",
    "log4j_message": "transportation service created with id=5.088856004746587",
    // extra fields...
}
```

We're getting pretty close to the format required by the aggregator, but we're still missing a few fields.

### Step 4: Namin' our project

One of the fields we're still missing is the `project` field. This should be a key that identifies our project/service/codebase such that Hyperion can identify which project these log entries belong to. Usually, this field is something stored alongside the log information in the data source. For illustration purposes however, we will be adding it ourselves.

Since we only have a single project, we just need to add a static value to every incoming message. That's what the [adder](/pipeline/plugins/adder) plugin is for! The following `adder.yml` plugin should be fairly self-explanatory:

```yaml
add:
  - key: "project"
    value: "mock-logging"
   
pipeline:
    manager-host: "localhost:3000"
    plugin-id: "Adder"
```

As always, ensure that you add the following to the end of your plugin manager config:

```yaml
  - id: "Adder"
    host: "tcp://localhost:3004"
```

Now, launching the adder is a classic:

```shell script
java -jar path/to/adder-all.jar adder.yml
```

You can probably already guess what our messages look like now:

```json
{
    "project": "mock-logging",
    "timestamp": "2020-06-02T10:03:44.000Z",
    "location": {
        "file": "src/main/java/com/sap/enterprises/server/impl/TransportationService.java",
        "line": "11",
    },
    "severity": "INFO",
    "log4j_message": "transportation service created with id=5.088856004746587",
    // extra fields...
}
```

We're almost done. We just need to add the `version` field and hook up the aggregator.

### Step 5: Versioning

It's very useful to know which version of the project actually generated the log line, as this allows us to only show metrics for relevant versions in the editor. Usually, we're expecting version info to be stored in ElasticSearch/other data sources, but we also have a default plugin for dynamically pulling this information from a git repository.

Let's assume that we have some CI/deploy system configured to always deploy the code that currently sits on the `master` branch. That means that we can query which commit currently sits at the head of master and attach that version to the messages (taking the assumption that the log was generated by the successfully deployed latest version of the codebase).

If all of these assumptions hold, we can use the [versiontracker](/pipeline/plugins/versiontracker) plugin to automatically pull the latest commit and attach it to the message. Put the following in `versiontracker.yml`. You may notice that the `mock-logging` key matches the project value that we configured in the previous step:

```yaml
projects:
    mock-logging:
        repository: https://github.com/nickyu42/mock-elastic-logging
        branch: refs/heads/master
        update-interval: 60

pipeline:
    manager-host: "localhost:3000"
    plugin-id: "VersionTracker"
```

As always, ensure that you add the following to the end of your plugin manager config:

```yaml
  - id: "VersionTracker"
    host: "tcp://localhost:3005"
```

And launch accordingly:

```shell script
$ java -jar path/to/versiontracker-all.jar versiontracker.yml
```

Our messages will now look like this:

```json
{
    "project": "mock-logging",
    "version": "ac362e0a33062a0eec28dcb9a51d439976a53b4a",
    "timestamp": "2020-06-02T10:03:44.000Z",
    "location": {
        "file": "src/main/java/com/sap/enterprises/server/impl/TransportationService.java",
        "line": "11",
    },
    "severity": "INFO",
    "log4j_message": "transportation service created with id=5.088856004746587",
    // extra fields...
}
```

We now have all the fields required for the aggregator. Time to set it up.

### Step 6: Aggregating

Now that we have our messages in a format that the aggregator understands, it is time to configure the aggregator. We will be using this simple config (place it in `aggregator.yml`), but you can view the full set of options [here](/aggregator/README.md). Do note that you will need a [PostgreSQL](https://www.postgresql.org/) database for the aggregator.

```yaml
database-url: "postgresql://localhost/postgres?user=postgres&password=mysecretpassword"
port: 8081
granularity: 10 # 10 seconds
aggregation-ttl: 604800 # 7 days

pipeline:
    manager-host: "localhost:3000"
    plugin-id: "Aggregator"
```

Finally, add the aggregator to the list of plugins in the plugin manager config. Your final plugin manager configuration should now look something like this:

```yaml
host: "tcp://localhost:3000"
plugins:
  - id: "ElasticSearch"
    host: "tcp://localhost:3001"
  - id: "Renamer"
    host: "tcp://localhost:3002"
  - id: "PathExtractor"
    host: "tcp://localhost:3003"
  - id: "Adder"
    host: "tcp://localhost:3004"
  - id: "VersionTracker"
    host: "tcp://localhost:3005"
  - id: "Aggregator"
    host: "tcp://localhost:3006"
```

Launching the aggregator works the same as launching plugins:

```shell script
$ java -jar path/to/aggregator-all.jar aggregator.yml
```

That's it! Your log messages should now be going from ES, through the pipeline, into the aggregator. You can confirm whether or not your aggregator is working by requesting the following URL:

```
http://localhost:8081/api/v1/metrics?project=mock-logging&file=src/main/java/com/sap/enterprises/server/impl/TransportationService.java&intervals=10,60
```

It should return a list of metrics encountered in the TransportationService file in the last 10 seconds and minute.

### Step 7: Intellij Setup

Now that we have the data in our aggregator, it is time to actually use it.

TODO: Plugin setup.

### Wrapping Up

So what have we actually created? Let's revisit the diagram from the beginning of this document, but with all of the plugins explicitly laid out.

![](https://i.imgur.com/cJRicZi.png)

This may seem like a complex setup, but the protocol that underlies the plugins is very optimized and allows thousands of messages handled per second on commodity hardware. By using a pipeline setup you can customize Hyperion for your own usecases, and even [write your own plugin](/docs/writing-java-kotlin-plugin.md) if needed.

The [example](example/) folder in this repository contains the files used in this tutorial. If you want to refer to the final working setup, you can reference that folder.

Want more information on how to use the default pipeline plugins bundled with Hyperion to configure your own pipeline? Check out the [example pipeline configurations](/doc/advanced-examples.md) documentation article for a list of plugins and the situations in which they can be used.
