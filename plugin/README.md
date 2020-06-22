# Hyperion - IntelliJ IDE Integration

![Gradle check Plugin](https://github.com/SERG-Delft/hyperion/workflows/Gradle%20check%20Plugin/badge.svg)

This project integrates the Hyperion pipeline with the IntelliJ IDEA IDE family. It is able to show the amount of logs triggered inline in the editor, based on information aggregated in the [aggregator](/aggregator). It is a reference implementation for how IDEs can be integrated with Hyperion.

![](https://i.imgur.com/0T3c4qL.png)

## User guide

Please check [step 7 of the hyperion setup](/docs/hyperion-setup.md#step-7-intellij-setup) for an up-to-date guide on how to get started with the IntelliJ plugin.

## Building & Running

To build the IntelliJ plugin, run `gradle plugin:buildPlugin`. The result will be located in `build/distributions/plugin.zip`.

To publish the IntelliJ plugin to the marketplace, run `gradle plugin:publishPlugin`. Note that this requires an authentication token from the serg-delft vendor and the plugin version needs to be unique.

To execute the tests and linting, run `gradle plugin:check`.

To launch a sandbox IDE with the plugin preinstalled, run `gradle plugin:runIde`.

