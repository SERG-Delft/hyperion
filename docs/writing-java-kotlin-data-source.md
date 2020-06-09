# Creating a new data source plugin using JVM plugin library

Creating a pipeline plugin in a JVM language is done by extending the shipped `AbstractPipelinePlugin` which is available in the plugin library on Maven. The library is written in [Kotlin](https://kotlinlang.org/) and is based around [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines).

The abstract class allows for easy creation of your own pipeline plugins for any languages running on the JVM. In this example we will guide you through creating the `reader` plugin in Kotlin — which reads input from stdin and pushes it to the pipeline. The full source code can be found [here](https://github.com/SERG-Delft/monitoring-aware-ides/tree/master/pipeline/plugins/reader). This plugin differs from the others in that it does not process data, but sends data from another source to the pipeline.

 Note that we use [Gradle](https://gradle.org/) as the build tool.

## 1. Plugin module structure
The easiest way to set up a plugin would be to just copy the directory structure of one of the existing plugins and renaming it as needed. To create the structure manually, start by creating a new directory and give it the name of your plugin. With Gradle we can create a new project by executing:

```shell script
$ mkdir reader
$ cd reader
$ gradle init --dsl kotlin
```

You can also add an optional `Dockerfile`.  
The file structure should look something like this in the end:

```
reader
├── src
│   ├── main
│   │   └── reader
│   └── test
│       └── reader
├── build.gradle.kts
├── Dockerfile
├── gradlew
└── gradle.bat

```
### build.gradle.kts
The build.gradle.kts script tells [Gradle](https://gradle.org/) how to build your plugin. The following `build.gradle.kts` template will be used.

```kotlin
plugins {
    application
    jacoco
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt").version("1.8.0")
    id("com.github.johnrengelman.shadow").version("5.2.0")
}

repositories {
    jcenter()
    maven {
        url("https://dl.bintray.com/serg-tudelft/monitoring-aware-ides")
    }
}

application {
    mainClassName = "filewatcher.Main"
}

dependencies {
    // load AbstractPipelinePlugin
    implementation("nl.tudelft.hyperion", "pipeline-common", "0.1.0")
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

### Dockerfile
For ease of use, a plugin can be run via Docker. This Dockerfile should launch the plugin with the given path to the configuration as environment variable. For our plugin it will look like:

```dockerfile
FROM openjdk:14
COPY build/reader-all.jar .
CMD java -jar reader-all.jar ${CONFIGPATH}
```

## 2. Plugin configuration

The plugin library uses YAML as the format for configuration files. The library also includes some utilities for parsing YAML files but any desired format could be used. For consistency, we will YAML in this example. The minimum required configuration for a plugin is the address of the plugin manager and a unique identifier for the plugin (see [protocol.md](https://github.com/SERG-Delft/monitoring-aware-ides/blob/more-documentation/docs/protocol.md)). The plugin library uses the following format for configuration.

```yaml
# config.yml
manager-host: "localhost:5555"
plugin-id: "Reader"
```

<!-- To extend this configuration with additional parameters needed for our plugin we will create a wrapper data class containing the pipeline plugin configuration. A new file should be added to the package, in our case we will call it 'FileWatcherConfiguration.kt'. The data class will contain any fields needed for the configuration of this plugin specifically and include the abstract plugin configuration as well. In our example, it looks like this:

```kotlin
// FileWatcherConfiguration.kt

import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Configuration for the file watcher plugin.
 *
 * @param file the path to the file to watch.
 * @param pipeline the general configuration for a pipeline plugin.
 */
data class FileWatcherConfiguration(
    val file: String,
    val pipeline: PipelinePluginConfiguration
)
``` -->

<!-- We can now add the additional information to our configuration file, after which it looks like this:

```yaml
file: "/var/log/foo.log"

pipeline:
  manager-host: "localhost:5555"
  plugin-id: "Watcher"
``` -->

In the next section we will see how the configuration is used in the new plugin.

## 3. Implementing the plugin

The only necessary function to implement is `onMessageReceived()`. But `onMessageReceived()` is used by plugins that transform the input, so it is not necessary to implement it for this plugin as it does not transform any input and will never be called.

```kotlin
// Reader.kt
class ReaderPlugin(
    private val config: PipelinePluginConfiguration
) : AbstractPipelinePlugin(config) {

    override suspend fun onMessageReceived(msg: String) = Unit

}
```

This plugin does not do anything currently, so lets add some functionality by overriding the run method to start a coroutine that reads from stdin.

```kotlin
// Reader.kt
class ReaderPlugin(...) : AbstractPipelinePlugin(...) {

    ...

    override fun run(context: CoroutineContext): Job {
        // Verify that we're the first step in the pipeline.
        if (!canSend || canReceive) {
            throw IllegalStateException(
                "The 'reader' plugin must be the first step in the pipeline."
            )
        }

        return CoroutineScope(Dispatchers.Default).launch {
            val mainJob = super.run(context)
            val readLoop = TODO("Add job that reads from stdin")

            // Sleep while workers are active
            try {
                mainJob.join()
            } finally {
                // cancel and cleanup
                mainJob.cancelAndJoin()
                readLoop.cancelAndJoin()
            }
        }
    }

    ...
}
```

You may notice the initial check if this plugin can send messages or not. The plugin itself does not configure in which step of the pipeline it is, this is communicated by the plugin manager. The `canSend` and `canReceive` properties are set by `AbstractPipelinePlugin` when the information is queried from the plugin manager, this can be manually done by calling `AbstractPipelinePlugin.queryConnectionInformation()` or automatically by using one of the helper functions (`runPipelinePlugin()`) in the library, which we will see later.

For now we need a coroutine that polls stdin for input.

```kotlin
// Reader.kt
class ReaderPlugin(...) : AbstractPipelinePlugin(...) {
    ...

    fun runReadLoop() = GlobalScope.launch {
        while (isActive) {
            val line = withContext(Dispatchers.IO) {
                readline()
            } ?: break

            send(line)
        }
    }

    ...
}
```

The `send()` function queues a message to be sent to the next plugin. Now that we implemented this we can replace the `TODO()` in `run()` with this function.

```kotlin
// Reader.kt
class ReaderPlugin(...) : AbstractPipelinePlugin(...) {
    
    ...

    override fun run(context: CoroutineContext): Job {
        ...
        return CoroutineScope(Dispatchers.Default).launch {
            val mainJob = super.run(context)
            val readLoop = runReadLoop()
            ...
        }
    }

    ...
}
```

Finally we can use the `runPipelinePlugin()` function to run the newly created plugin. The function takes the path to the config file and a reference to the plugin. As mentioned before, this is a helper function that automatically calls `queryConnectionInformation()` for us.

```kotlin
// Reader.kt
fun main(vararg args: String) = runPipelinePlugin(
    args[0],
    ::ReaderPlugin
)
```

Now that the logic is there, we can build and run the plugin.


## 4. Running the plugin

We can build this plugin with gradle by running:

```shell script
$ cd reader
$ gradle shadowJar
```

The resulting jar can be found in `build/reader-all.jar` which we can then execute with Java with the following, assuming that the config file is also in `/reader`:

```shell script
$ java -jar build/reader-all.jar config.yml
```

## 5. Testing your plugin

TODO


<a name="footnote1">1</a>: The `send()` function is suspending so it waits in the event that the queue is full.