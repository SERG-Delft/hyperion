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
                test/
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
    mainClassName = "nl.tudelft.hyperion.pipeline.addtext.Main"
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
                minimum = "0.6.toBigDecimal()
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
This configuration will load Kotlin, [Jacoco](https://www.eclemma.org/jacoco/) for testing, [Detekt](https://detekt.github.io/detekt/) for checkstyle and [shadowJar](https://github.com/johnrengelman/shadow) for building a jar with all dependencies included.
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

### 2. plugin configuration
### 3. plugin logic
### 4. plugin entry point
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