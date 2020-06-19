# Hyperion - IntelliJ IDE Integration

![Gradle check Plugin](https://github.com/SERG-Delft/hyperion/workflows/Gradle%20check%20Plugin/badge.svg)

This project integrates the Hyperion pipeline with the IntelliJ IDEA IDE family. It is able to show the amount of logs triggered inline in the editor, based on information aggregated in the [aggregator](/aggregator). It is a reference implementation for how IDEs can be integrated with Hyperion.

![](https://i.imgur.com/0T3c4qL.png)

## Building & Running

To build the IntelliJ plugin, run `gradle plugin:shadowJar`. The result will be located in `build/plugin-all.jar`.

To execute the tests and linting, run `gradle plugin:check`.

To launch a sandbox IDE with the plugin preinstalled, run `gradle plugin:runIde`.

