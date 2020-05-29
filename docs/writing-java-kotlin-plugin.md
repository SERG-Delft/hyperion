# Hyperion pipeline plugins
Hyperion pipeline plugins represent the building blocks of your pipeline. 
Each plugin receives every message as a string, can process it in any way it desires 
and pushes the altered message on to the next plugin or to the aggregator.
## Create your own JVM plugin in Kotlin
The shipped [AbstractPlugin] written in [Kotlin](https://kotlinlang.org/) 
allows for easy creation of your own pipeline plugins for any languages running on the JVM.
In this example we will guide you through creating a simple Kotlin plugin 
which adds "foo" (`addfoo`) to the end of every message it receives.

### 1. initial files
Start by creating a new directory in `/pipeline/plugins/`, give it the name of your plugin.
Add a `Dockerfile`, `config.yml`, `build.gradle.kts` and the directories `src/main/` and `src/test/`.
The file structure should look something like this:
```
pipeline/
    plugins/
       addfoo/
            src/
                main/
                test/
            build.gradle.kts
            config.yml
            Dockerfile
```
#### build.gradle.kts
#### config.yml
#### Dockerfile
TODO: The Dockerfile should laod SHIT
```dockerfile
FROM openjdk:14
ENV CONFIGPATH ${configpath}
COPY extractor-all.jar .
CMD java -jar extractor-all.jar ${CONFIGPATH}
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