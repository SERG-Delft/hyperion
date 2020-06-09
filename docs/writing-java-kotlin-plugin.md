# Writing a pipeline plugin in Java/Kotlin (or any JVM language)
Creating a custom pipeline plugin in a JVM language is easy with the `nl.tudelft.hyperion:pipeline-common` package.  
In this example we will guide you through creating a simple `addtext` plugin, written in [Kotlin](https://kotlinlang.org). 
The `addtext` plugin will append a configurable string to every message it receives. 
[Gradle](https://gradle.org/) will be used for building the project and  [Intellij IDEA](https://www.jetbrains.com/idea/) as development environment.

## 1. Bootstrap Kotlin project
Create a new Gradle project in Intellij, select `openjdk-14` as your project SDK and hit the `Kotlin/JVM` button.
Click on next and name your project and hit finish.
This will create a basic Kotlin project with Gradle as build tool.

Add the following snippets to `build.gradle`:
```
repositories {
    jcenter()
    maven {
        url "https://dl.bintray.com/serg-tudelft/monitoring-aware-ides"
    }
}

dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk8"
    implementation 'nl.tudelft.hyperion:pipeline-common:0.1.0'
    testCompile group: 'junit', name: 'junit', version: '4.12'
}
```
This will import the pipeline-common module from the Hyperion project,
 alongside the kotlin standard library and junit for testing.

Add `org.example.addtext` packages in both the `main/kotlin/` and `test/kotlin/` directories. 
Add a `Dockerfile` and `README.md`, they will be empty for now. 
Your projects directory structure should look something like:
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
README.md
Dockerfile
```

### Dockerfile
###### Adding a Dockerfile is recommended but optional. 
The Dockerfile is used for launching the plugin with a specified configuration.
This will enable easy pipeline setup using a `Docker compose` file.
By convention, it will load the path to the configuration file by reading the `CONFIGPATH` environment variable.
To be able to run the code you need to build a jar, 
for now we assume that this jar is called `addtext-all.jar` and is located in the root directory.
```dockerfile
FROM openjdk:14
ENV CONFIGPATH ${configpath}
COPY addtext-all.jar .
CMD java -jar addtext-all.jar ${CONFIGPATH}
```

### README
The README file presents what a plugin does, how you should configure it and how to run it.
You can use the README from the [`adder` plugin](../pipeline/plugins/adder/README.md) as an example for the `addtext` plugin.

## 2. plugin configuration
The minimum required configuration for a plugin is the configuration of the abstract plugin we will be extending. 
This configuration contains the identifier of the plugin and the location (host and port) of the plugin manager. 
Basic configuration looks like:

```yaml
pipeline:
  manager-host: "1.2.3.4:5678"
  plugin-id: "identifier"
```

To extend this configuration with additional parameters needed for our plugin we create a wrapper data class containing the pipeline plugin configuration. 
A new file should be added to the package, in our case we will call it 'AddTextConfiguration.kt'. 
The data class in it will contain any fields needed for the configuration of this plugin specifically and include the abstract plugin configuration as well. 
In our example, it looks like this:

```kotlin
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

For the parsing of the configuration, it is important to make sure the name of the field for the pipeline plugin configuration is 'pipeline'.
The names of the additional fields should simply correspond to the ones in the configuration file.
Alternatively, the @JsonProperty annotation can be used to specify a different name.

We can now add the additional information to our configuration, after which it looks like this:

```yaml
pipeline:
  manager-host: "1.2.3.4:5678"
  plugin-id: "identifier"

add: "The string to be added to incoming messages"
```

Any structure of additional parameters can be included in this way. 
Lists or additional classes may be helpful when parsing more complex structures. 
We could for example create a separate configuration class for our plugin to separate all the parameters from the actual wrapper class.

Furthermore, it might be useful to know that a list of elements in the configuration file is parsed as a list of the type of those elements. 
In our case, we could have used a `List<String>` type for the add field if we would have wanted to parse multiple strings to be added to messages. 
In that case the following would be an example of a parsable configuration:

```yaml
pipeline:
  manager-host: "1.2.3.4:5678"
  plugin-id: "identifier"

add:
  - "First string to be added"
  - "Second string to be added"
```

In the next section we will see how the configuration is used in the extended plugin.

## 3. plugin logic
To write the plugin itself and its logic, we create a class that extends from the AbstractPipelinePlugin class. 
This abstract class requires you to implement the process method, 
which simply takes a string and returns it after some processing has been done on it. 
We create a new file in the package again, which we will call `AddTextPlugin.kt`. 
Without the implementation it should look like this:

```kotlin
package org.example.addtext

import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin

/**
 * Class that extends the AbstractPipelinePlugin class and represents the text adder plugin
 */
class AddTextPlugin(private var config: AddTextConfiguration): AbstractPipelinePlugin(config.pipeline) {
    override suspend fun process(input: String): String {
        return input
    }
}
```

For now, any incoming messages will directly be returned without any manipulations. 
Notice how the configuration is used to pass the pipeline configuration to the AbstractPipelinePlugin and can be used in the process method with the extended parameters as well.

To add the functionality of the text adder plugin, 
we can simply concatenate the incoming message with the configured postfix and return it. 
This will be your final process method:

```kotlin
override suspend fun process(input: String): String {
    return input + config.add
}
```

###### The pipeline-common library introduces functions to alter JSON messages such as `findParent` and `findOrCreateChild`.

## 4. plugin entry point
To launch a plugin, the runPipelinePlugin method can be called. 
This method is part of the pipeline common utils, 
it takes a path to the configuration file and a class reference to the extended plugin to launch a new coroutine in which the plugin will run. 
Create `Main.kt` in the main kotlin package and add the following contents to it:

```kotlin
import nl.tudelft.hyperion.pipeline.runPipelinePlugin

fun main(vararg args: String) = runPipelinePlugin(
    args[0],
    ::AddTextPlugin
)
```

After creating the `Main.kt` file register the plugin as an application in the `build.gradle` file by adding:

```
plugins {
    id 'application'
}

application {
    mainClassName = 'org.example.addtext.Main.kt'
}
```
This will allow the plugin to be ran when it is built as a JAR file.
Running the main method directly with the configuration path as an argument will launch the plugin such that it will process any incoming messages and pass it on to the next plugin in the pipeline or the aggregator.


## 5. testing your plugin
To ensure reliability and correctness of the working of our plugin we should add unit tests. 
Unit tests aim to test for correctness of code while executing it (more info about [Software Testing](https://en.wikipedia.org/wiki/Software_testing)). 
For our plugin we will simply test whether the string we want is added to an incoming message. 
Create `AddTextPluginTest.kt` in `src/test/kotlin/org/example/addtext/` and copy in the following code:
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
        val addFooConfig = AddTextConfiguration("foo", pipeline) // configer our AddTextPlugin to add "foo"
        val addFoo = AddTextPlugin(addFooConfig) // create an instance of the AddTextPlugin

        val input = """Hey, I am """ // input for the process method
        val expected = """Hey, I am foo""" // expected output

        val ret = runBlocking { addFoo.process(input) } // run the process method on the input
        assertEquals(expected, ret) // checks if the output equals the expected input
    }
}
```
This simple test will check if the string "foo" is added to our test input "Hey, I am ". 

##6. Running your plugin
In order to run your plugin within the pipeline you could use a tool like [shadow](https://github.com/johnrengelman/shadow)
to build a 'fat' JAR including your plugin and its dependencies.
Use the Dockerfile to run your plugin inside a container with Docker or just launch the jar yourself.
Make sure the Dockerfile points to the correct path of the fat JAR when using Docker.
