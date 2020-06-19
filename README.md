![](https://i.imgur.com/OcUEJ6s.png)

This repository contains the source code for the various official parts of the Hyperion log aggregation and visualization pipeline. 

## What is Hyperion?

Hyperion integrates your current logging and devops systems directly with your IDE. During editing, you get live metrics on how often your logs trigger, how they trend over time and get warned of any outliers. By directly integrating these metrics into your editor, it becomes easier for developers to act on data without needing to actively request the metrics in a system such as Grafana or Kibana. For more information, please see [this paper](https://pure.tudelft.nl/portal/files/62265924/fse19.pdf) by Winter et al. describing the original system that Hyperion is based on.

Since every logging and monitoring stack is different, Hyperion is designed to be very flexible in the way it processes data. Data is ingested from sources such as LogStash, ElasticSearch or raw files and can flow through a configurable pipeline that adds metadata to the logs. Finally, these logs are aggregated and stored for a configurable interval, such that IDE integrations can query the metrics on-demand.

Hyperion is designed to be robust and fast. The initial release is able to handle up to 15 thousand log messages per second using commodity hardware. Additionally, the pipeline can scale horizontally by balancing load between different instances. Depending on the exact setup, logs can be aggregated within minutes of them occuring in production.

Hyperion **is**:
- A composable pipeline allowing any kind of logging setup to interface with it.
- A database of partially aggregated metrics detailing log behavior of your system.
- A system for showing metrics in your IDE.

Hyperion **is not**:
- A replacement for ElasticSearch, Loggly, your favorite log storage.
- A monitoring system for key performance metrics.
- A complete devops monitoring solution.

## Screenshots

![](https://i.imgur.com/mq1xyKW.gif)

## Documentation

There is documentation available for setting up Hyperion and for writing custom plugins, data sources and IDE integrations. Please see the following links:

- [Installing and configuring your first Hyperion pipeline](docs/hyperion-setup.md)
- [Example pipeline configurations for common devops setups](docs/advanced-examples.md)
- [Writing a new pipeline plugin in Java/Kotlin](docs/writing-java-kotlin-plugin.md)
- [Writing a new pipeline plugin in a different language](docs/writing-custom-plugin.md)
- [Writing a new data source in Java/Kotlin](docs/writing-java-kotlin-data-source.md)
- [The Hyperion pipeline protocol in more detail](docs/protocol.md)

If you encounter a problem not documented, feel free to open an issue.

## Projects

This repository is a monorepo that contains various sub-projects implementing Hyperion functionality. These projects are created by the Hyperion team and are guaranteed to be thoroughly tested and stable. However, Hyperions architecture allows for anyone to substitute parts of Hyperions pipeline as they want. For more information, see our [docs].

The following projects are in this repository:

- [aggregator](aggregator): The final stage of the Hyperion pipeline, aggregating incoming log messages and storing them into a database. Provides an API for IDE integrations to query metrics.
- [datasource/common](datasource/common): A common set of Java/Kotlin APIs that can be used to implement new Hyperion data sources.
- [datasource/plugins/elasticsearch](datasource/plugins/elasticsearch): A data source for Hyperion that pulls data from an ElasticSearch instance.
- [logstash-output-hyperion](logstash-output-hyperion): A plugin for logstash that allows it to output to the Hyperion pipeline. This plugin should be prefered over the ElasticSearch data source if you have a typical ELK stack.
- [pipeline/common](pipeline/common): A common set of Java/Kotlin APIs for implementing Hyperion pipeline transformation plugins.
- [pipeline/plugins/adder](pipeline/plugins/adder): A simple pipeline plugin that adds static values to incoming JSON messages.
- [pipeline/plugins/extractor](pipeline/plugins/extractor): A simple pipeline plugin that extracts values from string JSON fields into separate fields.
- [pipeline/plugins/loadbalancer](pipeline/plugins/loadbalancer): A pipeline plugin that allows for multiple instances of Hyperion pipeline plugins to be load-balanced transparently.
- [pipeline/plugins/pathextractor](pipeline/plugins/pathextractor): A simple pipeline plugin that transforms Java package names into their appropriate file path.
- [pipeline/plugins/renamer](pipeline/plugins/renamer): A simple pipeline plugin that renames JSON field names.
- [pipeline/plugins/versiontracker](pipeline/plugins/versiontracker): A simple pipeline plugin that attaches a version tag to incoming JSON data based on the latest commit in a Git repository.
- [pipeline/plugins/printer](pipeline/plugins/printer): A simple pipeline plugin that prints all messages passing through.
- [pipeline/plugins/rate](pipeline/plugins/rate): A simple pipeline plugin that logs the rate at which messages pass through the pipeline.
- [pipeline/plugins/reader](pipeline/plugins/reader): A simple pipeline plugin that pushes the contents of stdin to the pipeline for debugging purposes.
- [pipeline/plugins/stresser](pipeline/plugins/stresser): A simple pipeline plugin that can stress-test the pipeline by sending a single message as fast as possible.
- [plugin](plugin): An IntelliJ IDEA plugin that integrates with the aggregator to show metrics in the IDE.
- [pluginmanager](pluginmanager): A central server that manages the pipeline order and orchestrates communication between pipeline parts.

Additionally, [system-tests](system-tests) contains various simple Docker setups that test fully configured Hyperion pipelines. They are mainly for development and are likely not interesting for end-users.

## Building

Most projects within this repository use Gradle and Java 11. To get started with building Hyperion, simply install JDK 11. After installing, you will be able to compile a sub-component using the provided `./gradlew` wrapper.

To compile a sub-component, run `./gradlew <project name>:shadowJar`, where `<project name>` is a project as listed in [settings.gradle.kts](settings.gradle.kts). For example, to compile the aggregator you can run `./gradlew aggregator:shadowJar`. The resulting jar will be located in the `build` subfolder of the relevant project.

To run unit tests and the linter, you can run `./gradlew <project name>:check`.

For more information on building a specific component, please refer to the building documentation in the relevant README for that project.

## Contributing

Contributions are welcome! Simply create a PR outlining your changes and your motivation for them and we will try to respond as soon as possible. Just make sure that CI passes :)

## License

[Apache 2](/LICENSE)
