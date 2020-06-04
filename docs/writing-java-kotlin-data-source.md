# Creating a new data source plugin using JVM plugin library

Creating a pipeline plugin in a JVM language is done by extending the shipped `AbstractPipelinePlugin` written in [Kotlin](https://kotlinlang.org/). 

This abstract class allows for easy creation of your own pipeline plugins for any languages running on the JVM. In this example we will guide you through creating a `filewatcher` plugin watches a log file for new lines.  

## 1. Plugin module structure
The easiest way to set up a plugin would be to just copy the directory structure of one of the existing plugins and renaming it as needed. To create the structure manually, start by creating a new directory in `/pipeline/plugins/`, give it the name of your plugin. Add a `build.gradle.kts` and an optional `Dockerfile` and the directories `src/main/` and `src/test/`. The file structure should look something like this:
```
pipeline/plugins/
└── filewatcher
    ├── src
    │   ├── main
    │   │   └── filewatcher
    │   └── test
    │       └── filewatcher
    ├── build.gradle.kts
    └── Dockerfile
```
### build.gradle.kts
The build.gradle.kts script tells [Gradle](https://gradle.org/) how to build your plugin. Copy the following example script below into this file.

```kotlin
plugins {
    application
    jacoco
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt").version("1.8.0")
    id("com.github.johnrengelman.shadow").version("5.2.0")
}
application {
    mainClassName = "filewatcher.Main"
}
dependencies {
    // load AbstractPipelinePlugin
    implementation(project(":pipeline:common"))
}
jacoco {
    toolVersion = "0.8.5"
    reportsDir = file("$buildDir/jacoco")
}
tasks.jacocoTestReport {
    reports {
        xml.isEnabled = false
        csv.isEnabled = false
        html.destination = file("${buildDir}/jacocoHtml")
    }
}
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "BRANCH"
                minimum = "0.7".toBigDecimal()
            }
            limit {
                counter = "LINE"
                minimum = "0.6".toBigDecimal()
            }
        }
    }
}
tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
tasks.build {
    dependsOn(tasks.jacocoTestReport)
    dependsOn(tasks.shadowJar)
}
tasks.shadowJar {
    destinationDirectory.set(file("$buildDir"))
    archiveFileName.set("${archiveBaseName.get()}.${archiveExtension.get()}")
}
```
This configuration will load Kotlin, [Jacoco](https://www.eclemma.org/jacoco/) for testing, [Detekt](https://detekt.github.io/detekt/) for checkstyle and [shadowJar](https://github.com/johnrengelman/shadow) for building jars.

The newly created module should be registered in the top-level `settings.gradle.kts` file by adding an `include` at the end of the file. For example:

```kotlin
# settings.gradle.kts
...
include("pipeline:plugins:filewatcher")
```

### Dockerfile
For ease of use, a plugin can be run via Docker. This Dockerfile should launch the plugin with the given path to the configuration as environment variable. For our plugin it will look like:

```dockerfile
FROM openjdk:14
COPY build/addtext-all.jar .
CMD java -jar addtext-all.jar ${CONFIGPATH}
```

## 2. Plugin configuration

The plugin library uses YAML as the format for configuration files. The library also includes some utilities for parsing YAML files but any desired format could be used. For consistency, we will YAML in this example. The minimum required configuration for a plugin is the address of the plugin manager and a unique identifier for the plugin (see protocol (TODO)). The plugin library uses the following format for configuration.

```yaml
pipeline:
  manager-host: "1.2.3.4:5678"
  plugin-id: "identifier"
```

To extend this configuration with additional parameters needed for our plugin we will create a wrapper data class containing the pipeline plugin configuration. A new file should be added to the package, in our case we will call it 'FileWatcherConfiguration.kt'. The data class will contain any fields needed for the configuration of this plugin specifically and include the abstract plugin configuration as well. In our example, it looks like this:

```kotlin
// FileWatcherConfiguration.kt

import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Configuration for the text adder plugin.
 *
 * @param file the path to the file to watch.
 * @param pipeline the general configuration for a pipeline plugin.
 */
data class AddTextConfiguration(
    val file: String,
    val pipeline: PipelinePluginConfiguration
)
```

We can now add the additional information to our configuration file, after which it looks like this:

```yaml
file: "/var/log/foo.log"

pipeline:
  manager-host: "localhost:5555"
  plugin-id: "Watcher"
```

In the next section we will see how the configuration is used in the new plugin.

## 3. Implementing the plugin

TODO

## 4. Running the plugin

TODO

## 5. Testing your plugin

TODO