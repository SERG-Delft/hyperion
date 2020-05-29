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

## Create your own plugin for any programming language
TODO

## Included plugins
### Adder
### Extractor
### Pathextractor
### Renamer