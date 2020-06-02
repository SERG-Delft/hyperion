# Hyperion - Data Source Commons

This package contains a common abstract implementation in Kotlin for data source implementation. It is intended to be used as a library for other data source implementations to link against.

For instructions on how to create a new data source using this package, please see the following documentation article:

- [Writing a new data source in Java/Kotlin](/docs/writing-java-kotlin-data-source.md)

## Building & Running

To build the library, run `gradle datasource:common:shadowJar`. The result will be located in `build/common-all.jar`.

To execute the tests and linting, run `gradle datasource:common:check`.
