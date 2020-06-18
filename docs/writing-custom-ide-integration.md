# Writing a new IDE integration

> **Note**: This document is not meant as a tutorial on how to write a Hyperion integration for your favorite IDE. Instead, it is meant as a document that contains pointers for implementing your own frontend for the Hyperion aggregator.

This document will outline important information needed to create a new IDE implementation (or frontend) for the Hyperion pipeline. It describes how to communicate with the [aggregator](/aggregator/README.md) and how information should be resolved and shown to the user.

Roughly, a Hyperion frontend will function according to the following steps:
- Wait for the user to open a file.
- Query the aggregator for metrics for the opened file.
- Use the VCS to determine which metrics are relevant for the current file (ignore metrics that are not ancestors of the current version).
- Use the VCS to determine the local line number that corresponds to the tracked metrics.
- Render metrics for the relevant line inside the IDE.

The exact implementation of these steps will vary based on the IDE that you are integrating, but the general steps should remain the same.

## Querying the aggregator

The aggregator supports two different queries. The frontend can request the relevant metrics for a single file, and an aggregated metrics overview for the entire project. 

#### Querying file metrics

File metrics can be queried by making an HTTP GET request to

```
http://aggregator.address/api/v1/metrics?project=<project name>&file=<file path>&intervals=<intervals>
```

The `project` query parameter should match the project name as configured in the pipeline, and is used to determine which metrics to select. It is recommended to allow the user to configure this value on a per-project or per-workspace basis, as this is a value that will likely change based on which project the current user is working on.

The `file` query parameter should be the path of the file to be queried, relative to the repository root. Note that the endpoint will always return a result, even if the file has no metrics (or does not exist). 

The `intervals` query parameter should be a comma separated list of integers that define the intervals to query for. A value of `intervals=10,60,300` results in metrics reported for the last 10 seconds, last minute and last 5 minutes. Note that the result will clamp these intervals to the minimum granularity and the maximum TTL as configured in the aggregator. The returned interval values may therefore differ from the requested ones. It is recommended to allow the user to configure the metric intervals (either globally or on a per-project basis).

An example query may look like this:

```
/api/v1/metrics?project=mock-logging&file=src/main/java/com/sap/enterprises/server/impl/math/IntegerFactory.java&intervals=10,60,300
```

The response returned by the aggregator follows the following format:

```json
[{
    "interval": 60,
    "versions": {
        "bd5ed4a7680305b91d35ea56d7b103c1340dace3": [{
            "line": 10,
            "count": 20,
            "severity": "INFO"
        }],
        "8050421326b4a92a3fbe2f4d566ed72a940145db": [{
            "line": 20,
            "count": 1,
            "severity": "DEBUG"
        }]
    }
}, {
    "interval": 120,
    "versions": {
        "bd5ed4a7680305b91d35ea56d7b103c1340dace3": [{
            "line": 10,
            "count": 20,
            "severity": "INFO"
        }],
        "8050421326b4a92a3fbe2f4d566ed72a940145db": [{
            "line": 20,
            "count": 1,
            "severity": "DEBUG"
        }]
    }
}]
```

For every queried interval, an object is returned that contains the versions for which the aggregator has data in that specified interval. For the above example, the data should be interpreted as saying that in the last minute, version `bd5ed4a7680305b91d35ea56d7b103c1340dace3` had 20 INFO logs occuring at line 10, while version `8050421326b4a92a3fbe2f4d566ed72a940145db` had a single DEBUG log occuring at line 20.

A frontend should resolve which versions are relevant for the current working state (such as checking if the versions are ancestors of the current version), and tally the results accordingly. Also note that the line number returned by the query represents the line number of the file at that specific version, and that the local version may have moved the line in the meantime.

#### Querying metric histograms

The aggregator also supports an optional feature for querying a histogram of log types for either a specific file or the entire project. The histogram metrics can be requested by making an HTTP GET request to

```
http://aggregator.address/api/v1/metrics/period?project=<project name>&relative-time=<time to show in histogram>&steps=<number of steps to divide the time>[&file=<file to query>]
```

The `project` query parameter should match the project name as configured in the pipeline, and is used to determine which metrics to select. It is recommended to allow the user to configure this value on a per-project or per-workspace basis, as this is a value that will likely change based on which project the current user is working on.

The `relative-time` query parameter specifies the amount of time that should be taken total, which is then divided into `steps` number of steps. For example, specifying `relative-time=60` and `steps=6` will return 6 bins that each span 10 seconds.

The `file` query parameter is an optional parameter that allows you to only return metrics for a single file, instead of the entire project. It should be the path of the file, relative to the repository root.

On success, the aggregator will return a JSON payload that looks like:

```json
{
    "interval": 86400,
    "results": [
        {
            "startTime": 1591529339,
            "versions": {}
        },
        {
            "startTime": 1591615739,
            "versions": {
                "bd5ed4a7680305b91d35ea56d7b103c1340dace3": [
                    {
                        "line": 4,
                        "severity": "WARN",
                        "count": 20,
                        "file": "src/main/java/com/sap/enterprises/server/impl/TransportationService"
                    },
                    {
                        "line": 11,
                        "severity": "DEBUG",
                        "count": 10,
                        "file": "src/main/java/com/sap/enterprises/server/impl/TransportationService"
                    },
                    {
                        "line": 11,
                        "severity": "ERROR",
                        "count": 10,
                        "file": "src/main/java/com/sap/enterprises/server/impl/TransportationService"
                    },
                    {
                        "line": 11,
                        "severity": "INFO",
                        "count": 2,
                        "file": "src/main/java/com/sap/enterprises/server/impl/TransportationService"
                    }
                ]
            }
        },
        {
            "startTime": 1591702139,
            "versions": {}
        }
    ]
}
```

The `interval` property contains the "width" of every result bucket. In most cases, this is equal to `relative-time`/`steps` (although optionally rounded to the minimum granularity of the aggregator). Every result bucket contains the start time of that bucket (with the end time computable using `startTime` + `interval`), alongside the metrics per version known to the aggregator. Note that the `"file"` field in the metrics is only returned if querying on a project-wide basis.

## Resolving line locations

One of the main challenges for a frontend implementation is the logic for determining the current log location of a logging entry in the local working state. As code constantly changes, there's no guarantee that a specific line in the version currently running in production still corresponds to the same line in the local version of the code. As such, it is **highly** recommended that frontend implementations have a way to attempt to trace the line back to its origin across versions.

The reference implementation for IntelliJ IDEA has such a system by using the information output by `git blame`. The way in which it works is reproduced here for reference purposes, but it should be noted that this system is not perfect and that it is recommended to see if the IDE/editor has built-in systems for tracking line changes (both in the current editor relative to the version on disk, and between versions) before resorting to this system.

If the aggregator returns logs at line 10 of file `Test.java` on commit `bd5ed4a7680305b91d35ea56d7b103c1340dace3`, the reference implementation runs the following commands to resolve the line in the current version:

**Figure out which commit introduced the line:**
```shell script
$ git blame bd5ed4a7680305b91d35ea56d7b103c1340dace3 -L10,10 -M1 -C -C -w -n --porcelain -- Test.java
```
This will output the git commit hash that introduced line 10 of `Test.java` in `bd5ed4a`, along with the original line number where that specific line was added.

**Find the current location of that commit in the working tree:**
```shell script
$ git blame -p -l -t -w -- Test.java
```

One of the output lines will start with the commit hash discovered in the previous step. It will be followed by the current line and the original line. The current line number is the current position of the original line.