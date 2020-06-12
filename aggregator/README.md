# Hyperion - Aggregator

![Gradle check Aggregator](https://github.com/SERG-Delft/hyperion/workflows/Gradle%20check%20Aggregator/badge.svg)

The aggregator is the final step in the Hyperion pipeline. It receives incoming events from the pipeline and aggregates them by project, version and location. It exposes an HTTP API that IDE implementations can use to query the aggregated values.

## Dependencies

The aggregator requires a PostgreSQL database. During development, such a database can easily be provisioned using Docker:

```shell script
docker run --name some-postgres -p 5432:5432 -e POSTGRES_PASSWORD=mysecretpassword -d postgres
```

In production, it is recommended to run the database on a different machine. It is likely not needed to shard this database, unless either the TTL or granularity of the aggregation is set to extreme values.

## Building & Running

To build the aggregator, run `gradle aggregator:shadowJar`. The result will be located in `build/aggregator-all.jar`.

To execute the tests and linting, run `gradle aggregator:check`.

To run a compiled version of the aggregator, simply launch it using Java:

```shell script
java -jar build/aggregator-all.jar
```

## Configuration

The aggregator expects a configuration YAML file located at `./configuration.yaml` or in the path defined in the `HYPERION_AGGREGATOR_CONFIG` environment variable.

The following configuration options are accepted:

```yaml
# The URL for the postgres database. This is a JDBC URL without the 
# jdbc: prefix. For information on the syntax, see the following:
# https://jdbc.postgresql.org/documentation/head/connect.html
database-url: "postgresql://postgres/postgres?user=postgres&password=mysecretpassword"

# The port on which the aggregator will expose its API. It is recommended
# that this be put behind a reverse proxy such as NGINX, as the built-in 
# web server is quite bare-bones and does not support https.
port: 8080

# The granularity in seconds of the intermediate aggregates that the
# aggregator produces. This defines the minimum increment in seconds 
# that can be requested using the API, as well as the maximum time a
# log can be buffered in the aggregator before it shows up in the API.
# It is recommended to set this to 30-60 seconds, as this allows querying
# with a small granularity without significantly increasing the amount
# of data stored in the database. Halving this value will double the 
# amount of data stored in the database.
granularity: 60 # a minute

# The amount of time that aggregated values should be stored in the database
# before they should be purged. This is the maximum time you are able to 
# look back at rates/totals. Please note that Hyperion should not be the
# only place you store your logs: even if you set this to a smaller value the
# logs should be available elsewhere in case you need to query for values that
# are longer ago. The recommended value for this property is somewhere in the 
# range of a few days to a few weeks at most. Doubling this property will double
# the amount of data stored in the database.
aggregation-ttl: 604800 # 7 days

# Whether or not incoming timestamps should be validated to verify whether
# they lie within the current intermediate aggregation as determined by the
# granularity. If this is set to `true` (the default value), messages will
# not be aggregated if they are too old (their granularity window has already
# passed) or if the timestamp is missing entirely. If this is set to `false`,
# all received messages will be assumed to have occurred in the current
# granularity window, regardless of their actual timestamp value.
verify-timestamp: false

# Various settings needed for the aggregator to interact with the pipeline,
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
    plugin-id: "Aggregator"
```

## Input Format

The aggregator expects JSON input messages that adhere to the following format. All additional fields will be ignored.

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

The `timestamp` value is required if `verify-timestamp` is set to true. If timestamp verification is turned off, the
 timestamp value can be ignored.

If any of these fields are invalid or otherwise fail to parse, a warning is logged and the message is ignored. Please note that such warnings are logged on every single invocation, so they may get out of hand quickly depending on how many erroneous requests arrive.
