# Writing a new pipeline plugin in Java/Kotlin
Creating a pipeline plugin in a JVM language is done by extending on the shipped `AbstractPipelinePlugin` written in [Kotlin](https://kotlinlang.org/). This abstract class allows for easy creation of your own pipeline plugins for any languages running on the JVM. In this example we will guide you through creating a simple `addtext` plugin, written in Kotlin. The `addtext` plugin will append a configurable string to every message it receives.

## 1. initial files
Start by creating a new directory in `/pipeline/plugins/`, give it the name of your plugin (`addtext in this case`). Add a `Dockerfile`, `config.yml`, `build.gradle.kts` and the directories `src/main/` and `src/test/`. The file structure should look something like this:
```
pipeline/
    plugins/
       addtext/
            src/
                main/
                    packageName/
                test/
                    packageName/
            build.gradle.kts
            config.yml
            Dockerfile
```
### build.gradle.kts
The build.gradle.kts script tells [Gradle](https://gradle.org/) how to build your plugin. Copy the script below into this file, this build script will include, 
```gradle
plugins {
    application
    jacoco
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt").version("1.8.0")
    id("com.github.johnrengelman.shadow").version("5.2.0")
}

application {
    mainClassName = "addtext.Main"
}

dependencies {
    // testing
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.3.5")

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
    destinationDir = File("./build/addtext-all.jar");
}
```
This configuration will load Kotlin, [Jacoco](https://www.eclemma.org/jacoco/) for testing, [Detekt](https://detekt.github.io/detekt/) for checkstyle and [shadowJar](https://github.com/johnrengelman/shadow) for building jars.
Do not forget to register your module in the top-level `settings.gradle.kts` file by adding: `pipeling:plugins:addfoo` at the end of the file.

### config.yml
This is where you will specify the required configurations for your module. It is meant as an example configuration for developers which want to use your plugin. We will cover configuration in detail in step 2. Our example config.yml will look like:"
```
pipeline :
  manager-host: "1.2.3.4:4567"
  plugin-id : "myplugin"

add: "foo"
```

### Dockerfile
To enable ease of use, every plugin should include a Dockerfile. This Dockerfile should launch the plugin with the given path to the configuration as environment variable. For our plugin it will look like":
```dockerfile
FROM openjdk:14
ENV CONFIGPATH ${configpath}
COPY addtext-all.jar .
CMD java -jar addtext-all.jar ${CONFIGPATH}
```

## 2. plugin configuration
The minimum required configuration for a plugin is the configuration of the abstract plugin we will be extending. This configuration contains the identifier of the plugin and the location (host and port) of the plugin manager. That is why the 'config.yml' file introduced earlier should at least contain the following information:

```yaml
pipeline:
  manager-host: "1.2.3.4:5678"
  plugin-id: "identifier"
```

To extend this configuration with additional parameters needed for our plugin we create a wrapper data class containing the pipeline plugin configuration. A new file should be added to the package, in our case we will call it 'AddTextConfiguration.kt'. The data class in it will contain any fields needed for the configuration of this plugin specifically and include the abstract plugin configuration as well. In our example, it looks like this:

```java
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Configuration for the text adder plugin
 * @param add The string to be added to any incoming messages
 * @param pipeline The general configuration for a pipeline plugin
 */
data class AddTextConfiguration(
    val add: String,
    val pipeline: PipelinePluginConfiguration
)
```

For the parsing of the configuration, it is important to make sure the name of the field for the pipeline plugin configuration is 'pipeline'. The names of the additional fields should simply correspond to the ones in the configuration file. Alternatively, the @JsonProperty annotation can be used to specify a different name.

We can now add the additional information to our configuration file, after which it looks like this:

```yaml
pipeline:
  manager-host: "1.2.3.4:5678"
  plugin-id: "identifier"

add: "The string to be added to incoming messages"
```

Any structure of additional parameters can be included in this way. Lists or additional classes may be helpful when parsing more complex structures. We could for example create a separate configuration class for our plugin to separate all the parameters from the actual wrapper class.

Furthermore, it might be useful to know that a list of elements in the configuration file is parsed as a list of the type of those elements. In our case, we could have used a ListOf\<String> type for the add field if we would have wanted to parse multiple strings to be added to messages. In that case the following would be an example of a parsable configuration:

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
To write the plugin itself and its logic, we create a class that extends from the AbstractPipelinePlugin class. This abstract class requires you to implement the process method, which simply takes a string and returns it after some processing has been done on it. We create a new file in the package again, which we will call 'AddTextPlugin.kt'. Without the implementation it should look like this:

```java
import nl.tudelft.hyperion.pipeline.AbstractPipelinePlugin

/**
 * Class that extends the AbstractPipelinePlugin class and represents the text adder plugin
 */
class AddTextPlugin(private var config: Configuration): AbstractPipelinePlugin(config.pipeline) {
    override suspend fun process(input: String): String {
        return input
    }
}
```

For now, any incoming messages will directly be returned without any manipulations. Notice how the configuration is used to pass the pipeline configuration to the AbstractPipelinePlugin and can be used in the process method with the extended parameters as well.

To add the functionality of the text adder plugin, we can simply concatenate the incoming message with the configured postfix and return it. The process method then looks like this:

```java
override suspend fun process(input: String): String {
    return input + config.add
}
```

## 4. plugin entry point
To launch a plugin, the runPipelinePlugin method can be called. This method is part of the pipeline common utils, it takes a path to the configuration file and a class reference to the extended plugin to launch a new coroutine in which the plugin will run. As an example we create a simple main method that launches our new plugin:

```java
import nl.tudelft.hyperion.pipeline.runPipelinePlugin

fun main(vararg args: String) = runPipelinePlugin(
    args[0],
    ::AddTextPlugin
)
```

Running the main method with the configuration path as a program argument will launch the plugin such that it will process any incoming messages and pass it on to the next plugin in the pipeline or the aggregator.

## 5. testing your plugin
The `build.gradle.kts` provides support for both static and dynamic analysis. [Detekt](https://detekt.github.io/detekt/) is a static analysis tool for Kotlin and makes sure that your functions aren't too long, too complex or weirdly named. Detekt requires no further setup, if you want to change which rules Detekt applies to check your code add a  [detekt-config.yml](https://arturbosch.github.io/detekt/configurations.html) file. You can run the checkstyle analysis by running the Gradle plugins:addtext:detekt task.

To ensure reliability and correctness of the working of our plugin we should add some unit tests. Unit tests aim to test for correctness of code while executing it (more info about [Software Testing](https://en.wikipedia.org/wiki/Software_testing)). For our plugin we will simple test whether the string we want is added to an incoming message. Create the `AddTextPluginTest.kt` file in `src/test/addtext/` and copy in the following code:
```Kotlin
package plugins.textadder

import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions

class AddTextPluginTest {
    private val pipeline = PipelinePluginConfiguration("addfoo", "localhost") // default pipeline plugin configuration
    
    @Test
    fun `add foo to message`() {
        val addFooConfig = Configuration(pipeline, "foo") // configer our AddTextPlugin to add "foo"
        val addFoo = AddTextPlugin(addFooConfig) // create an instance of the AddTextPlugin

        val input = """Hey, I am """ // input for the process method
        val expected = """Hey, I am foo""" // expected output

        val ret = runBlocking { adder.process(input) } // run the process method on the input
        Assertions.assertEquals(expected, ret) // checks if the output equals the expected input
    }
}
```
This simple test will check if the string "foo"is added to our test input "Hey, I am ". Test can be executed using the pre-loaded `jUnitPlatform` (or `JUnit5`) by running the Gradle task `pipeline:plugins:addtext:test`. [Jacoco](https://www.eclemma.org/jacoco/) is a tool which can run all the tests for a specific module and creates a nice report with coverage details. The coverage report can be created by running the Gradle task `pipeline:plugins:addtext:jacocoTestReport`.

Automatic testing is a must when the codebase grows larger. To setup [Github Actions](https://github.com/features/actions) CI add `pipeline_addtext_check.yml` in the `.github/workflows/` directory located in the top directory of the Hyperion project with the following contents:
```workflow
# Runs Gradle Check on the pipeline addtext plugin
# Will be triggered on every pull request and push to master
# Only commences on check if the code changed
name: Gradle check pipeline addtext plugin

on:
  pull_request:
    paths:
      - 'pipeline/plugins/addtext/**'
  push:
    branches:
      - master

jobs:
  gradle-check:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v2

      - uses: actions/setup-java@v1
        with:
          java-version: 11

      - uses: actions/cache@v1
        with:
          path: ~/.gradle/caches
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
          restore-keys: |
            ${{ runner.os }}-gradle-

      - name: gradle check plugin
        uses: eskatos/gradle-command-action@v1
        with:
          arguments: :pipeline:plugins:addtext:check
```
This script will trigger the Gradle check task for the addtext plugin everytime a new pull request is made and the code changed in this plugin. It will additionally run on every push to the master branch as well. The Gradle check task will run static anaylis and the tests. It will make sure the coverage of the unit tests is at least 70% Branch coverage and 60% line coverage. When this level of coverage is not reached, the CI will fail. Aiming for a higher amount of coverage will generally improve the region for errors in your code. I some situation however achieving these coverage numbers is infeasible and should therefore be adjusted. You can do this by editing the `jacocoTestCoverageVerification` task in the `build.gradle.kts` file. The code fragment below shows the default settings for code coverage, you can lower these by adjusting the `minimum` numbers.
```Kotlin
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
```