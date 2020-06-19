# Writing a pipeline plugin in Java/Kotlin (or any JVM language)

Since Hyperion is written in [Kotlin](https://kotlinlang.org), we offer first-class support for easily creating plugins in any JVM language. This tutorial will walk you through creating a simple plugin in Kotlin that will append a fixed string to every incoming message.

For dependency management, we will be using [Gradle](https://gradle.org/). If you prefer to use Maven, the Hyperion libraries are also published in MavenCentral. For development, we will be using the [Intellij IDEA](https://www.jetbrains.com/idea/) IDE.

This tutorial assumes some familiarity with the Hyperion pipeline model and how the Hyperion protocol works. We recommend you read [Installing and configuring your first Hyperion pipeline](/docs/hyperion-setup.md) and [The Hyperion pipeline protocol in more detail](/docs/protocol.md) first if you haven't already.

Want to create a plugin in a different language? Check out [Writing a new pipeline plugin in a different language](/docs/writing-custom-plugin.md).

## 1. Project Setup

We will start with creating a new project for the plugin. Within IntelliJ, create a new Gradle project, select an SDK of at least version 11 (`openjdk-14` recommended) and ensure the `Kotlin/JVM` option is selected. You can pick any name and location for the created project, but leave the package as `org.example` (or substitute `org.example` for your own package in the rest of this tutorial).

![](https://i.imgur.com/nTYrSq1.png)

We will need some dependencies for creating our plugin. Update your `build.gradle` by updating the `dependencies` section according to the following snippet. This will import our Hyperion libraries, the Kotlin standard libary, and [JUnit](https://junit.org/junit4/) for testing.

```groovy
dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8"
    implementation "com.github.serg-delft.hyperion:pipeline-common:0.1.0" // <- added!
    testCompile group: "junit", name: "junit", version: "4.12"
}
```

Additionally, make sure to create packages for our code. Since we will be creating a plugin for adding text, let's name it `org.example.addtext`. Create such packages in both the `src/main/kotlin` and `src/test/kotlin` folders.

Your directory structure should now look something like this:

```
gradle/
src/
    main/
        kotlin/
            org.example.addtext/
    test/
        kotlin/
            org.example.addtext/
build.gradle
gradle.properties
gradlew
gradlew.bat
settings.gradle
```

Now that we have our project configured properly, let's start actually writing our plugin!

## 2. Parsing Configuration

By convention, a Hyperion plugin is configured using a simple yaml file. Even if we don't have any additional configuration options, we will still need to parse a configuration since we need to know the location of the plugin manager (see the [Hyperion protocol](/docs/protocol.md) to understand why we need a plugin manager). Luckily, the Hyperion plugin library includes helper methods for easily parsing configuration files.

Since our plugin will be adding a static text to the end of all incoming messages, we will want to offer a configuration option for specifying _what_ needs to be appended. As such, we probably want a configuration that looks something like this:

```yaml
add: "My text to add"

pipeline:
  manager-host: "1.2.3.4:5678"
  plugin-id: "identifier"
```

We can parse such a configuration fairly easily using the tools Hyperion provides us. Let's first create a class that represents the parsed configuration. Create a new Kotlin file in your main source `org.example.addtext` package and call it `AddTextConfiguration.kt`. Add the following to the created file:

```kotlin
package org.example.addtext

import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Configuration for the text adder plugin
 * @param add The string to be added to any incoming messages
 * @param pipeline The general configuration for a pipeline plugin
 */
data class AddTextConfiguration(
    val pipeline: PipelinePluginConfiguration
    val add: String
)
```

That's all we need! The Hyperion library will use [Jackson](https://github.com/FasterXML/jackson) to parse the configuration YAML and put the results in a new instance of the `AddTextConfiguration` class. Note that the field names of the class match the property names in the configuration. By convention, every plugin has a `pipeline` property, using the `PipelinePluginConfiguration` type imported from the Hyperion library.

If you want to parse more "advanced" configs, such as maps, lists or nested fields, check out some of the configuration classes of the reference plugins: [extractor](https://github.com/SERG-Delft/hyperion/blob/b879392cb1def187a79fa907a74eb5b472b4c2f6/pipeline/plugins/extractor/src/main/kotlin/nl/tudelft/hyperion/pipeline/extractor/Configuration.kt), [versiontracker](https://github.com/SERG-Delft/hyperion/blob/b879392cb1def187a79fa907a74eb5b472b4c2f6/pipeline/plugins/versiontracker/src/main/kotlin/nl/tudelft/hyperion/pipeline/versiontracker/Configuration.kt). We also recommend checking out the [Jackson](https://github.com/FasterXML/jackson) documentation, as annotations can be used to change how Jackson (de)serializes your config.

## 3. Transforming Input

Now that we are able to parse a configuration, let's actually write the plugin stage that will take incoming messages, append our configured text and send them to the next stage in the pipeline. The Hyperion plugin library provides a `TransformingPipelinePlugin` class for exactly this purpose. We can subclass it and provide our own logic for transforming the input.

For now though, lets start with a plugin that does nothing. Create a new `AddTextPlugin` class in the `org.example.addtext` package and add the following:

```kotlin
package org.example.addtext

import nl.tudelft.hyperion.pipeline.TransformingPipelinePlugin

/**
 * The main plugin class that will handle transformation of the
 * incoming messages according to the specified [AddTextConfiguration].
 */
class AddTextPlugin(private var config: AddTextConfiguration) : TransformingPipelinePlugin(config.pipeline) {
    // This function will be invoked by the Hyperion libraries to do
    // the actual tranformation. For now, we will just return the input.
    override suspend fun process(input: String): String {
        return input
    }
}
```

Notice how we also provide `config.pipeline` to the Hyperion libraries. It will use that configuration to automatically take care of talking to the plugin manager and handle the setup of connections with the previous and next plugin.

Let's update our `process` function to actually do what we set out to do. `config.add` contains the text we want to add, so we can just append it to the end like so:

```kotlin
override suspend fun process(input: String): String {
    return input + config.add
}
```

## 4. Plugin Entry Point

Now that we have created our `AddTextPlugin`, we need to be able to run it. As always, the Hyperion library contains a simple function that helps us out. Let's create a `Main.kt` file in `org.example.addtext` and add the following:

```kotlin
package org.example.addtext

import nl.tudelft.hyperion.pipeline.runPipelinePlugin

fun main(vararg args: String) = runPipelinePlugin(
    args[0],
    ::AddTextPlugin
)
```

As you might have guessed from the code, this will start the `AddTextPlugin` pipeline plugin, using the first passed argument (`args[0]`) as the path to the configuration file. By convention, Hyperion plugins receive the configuration file path as the first argument when launched.

We will also need to make sure that Gradle knows that this is our main file. Update your `build.gradle` to add the application plugin and to specify the main class:

```groovy
plugins {
    id "application"
}

application {
    mainClassName = "org.example.addtext.MainKt"
}
```

The application plugin will set up the necessary files such that we can build a `.jar` file that will run the plugin when launched.

## 5. Testing

We should add some tests to our plugin to ensure it works the way we expect it to. While it may sound excessive to create tests for such a simple plugin, it is a good practice to always add tests, even if the code looks trivial.

We can create a simple `AddTextPluginTest.kt` in our `src/test/kotlin` folder (in the appropriate package). Edit it to contain the following:

```kotlin
package org.example.addtext

import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import org.junit.Assert.assertEquals
import org.junit.Test

class AddTextPluginTest {
    private val pipeline = PipelinePluginConfiguration("addfoo", "localhost") // default pipeline plugin configuration

    @Test
    fun `add foo to message`() {
        val addFooConfig = AddTextConfiguration("foo", pipeline) // configure our AddTextPlugin to add "foo"
        val addFoo = AddTextPlugin(addFooConfig) // create an instance of the AddTextPlugin

        val input = "Hey, I am " // input for the process method
        val expected = "Hey, I am foo" // expected output

        val ret = runBlocking { addFoo.process(input) } // run the process method on the input
        assertEquals(expected, ret) // checks if the output equals the expected input
    }
}
```

This class contains a single test that asserts that calling `process` will append `"foo"` to the end of the test. If everything is correct, running the test will pass without problems. You can try adding some more tests, such as appending an empty string or appending a really long string.

## 6. Packaging Your Plugin

Now that the plugin is complete, let's make sure that we can create a single contained jar that anyone can run. We can do this by using the [shadow](https://github.com/johnrengelman/shadow) Gradle plugin. It will ensure that all of our dependencies are copied into a single "uberjar", that is able to run standalone. Configuring shadow is as simple as adding the following to your `plugins` section inside `build.gradle`:

```groovy
plugins {
    id 'com.github.johnrengelman.shadow' version '5.2.0'
}
```

That's it! You can now run `./gradlew shadowJar` to create a single jar that contains all dependencies. The result will be in `build/libs`.

## 7. Polishing

While the plugin is now functional and can be packaged, it is good practice to include a README and Dockerfile setup for the plugin. Other optional improvements could include setting up a CI, using a linter such as [Detekt](https://detekt.github.io/detekt/) and tracking test coverage.

For the README, it is recommended to document all configuration options, instructions on how to build the plugin, a usage example, and instructions for contribution. For inspiration, see the documentation for the [adder](/pipeline/plugins/adder/README.md) plugin.

It is also recommended to add a Dockerfile, as this allows you to push the plugin to the Docker registry such that you (and others) can easily set up pipelines using the pushed image. The recommended Dockerfile for a Hyperion plugin looks like this:

```dockerfile
FROM openjdk:14
ENV CONFIGPATH ${configpath}
COPY [path to your built -all.jar] .
CMD java -jar addtext-all.jar ${CONFIGPATH}
```

Make sure to update the path to your shadow jar as appropriate. For the example setup given, it'd be `build/addtext-1.0-SNAPSHOT-all.jar`. This Dockerfile uses the CONFIGPATH environment variable to specify the path to the config.

# Extra: Testing Your Plugin

If you want to test your plugin as part of a real pipeline, there's a pair of plugins that are specifically designed for this purpose. The [reader](/pipeline/plugins/reader) plugin reads input from stdin and sends it into the pipeline, while the [printer](/pipeline/plugins/printer) plugin prints any messages it receives. As such, if you want to test your plugin in a real pipeline, you can set up a plugin manager that uses a reader as input, routes it to your plugin, then routes it to the printer plugin.

# Other APIs

The Hyperion library additionally exports some other APIs that make it easy to do common tasks required from pipeline plugins. This reference contains some examples, although it is recommended to check both the [source code](/pipeline/common) of the commons library, as well as the source code of a plugin that is similar to what you want to build.

**`AbstractPipelinePlugin`**: If you want more control of how your plugin functions, consider overriding `AbstractPipelinePlugin` instead. Unlike the `TransformingPipelinePlugin`, this class does not require the plugin to have both a predecessor and a successor in the pipeline. The `canSend`, `canReceive` and `send` functions can be used to query the position of the plugin within the pipeline, and to manually send data. Note that you should only introduce new data through send if you are the first step in the pipeline (a data source).

**`readJSONContent`**: Helper function that parses the specified content as JSON and attempts to convert it to the given type. Internally uses the Jackson library. If you just need to convert some JSON to text and don't need fancy types, parsing or validation, this is your function.

**`findOrCreateChild` and `findParent`**: Helper functions that operate on Jackson tree types and allow querying of a field even in nested objects. See [the renamer plugin](https://github.com/SERG-Delft/hyperion/blob/9d906fd78997052795f7bd4d0b0df100cb6a6758/pipeline/plugins/renamer/src/main/kotlin/nl/tudelft/hyperion/pipeline/renamer/Rename.kt#L19-L43) for an example on how they are used.