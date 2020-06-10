import org.gradle.api.artifacts.dsl.DependencyHandler

/**
 * Adds jackson dependencies to the current dependency scope, optionally
 * including kotlin, yaml and joda dependencies.
 */
fun DependencyHandler.jackson(
    withKotlin: Boolean = true,
    withYAML: Boolean = true,
    withJoda: Boolean = false
) {
    add("implementation", "com.fasterxml.jackson.core:jackson-databind:$JACKSON_VERSION")

    if (withKotlin) {
        add("implementation", "com.fasterxml.jackson.module:jackson-module-kotlin:$JACKSON_VERSION")
    }

    if (withYAML) {
        add("implementation", "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$JACKSON_VERSION")
    }

    if (withJoda) {
        add("implementation", "com.fasterxml.jackson.datatype:jackson-datatype-joda:$JACKSON_VERSION")
    }
}

/**
 * Adds the kotlin coroutine dependency.
 */
fun DependencyHandler.coroutines() {
    add("implementation", "org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$KOTLIN_COROUTINES_VERSION")
}

/**
 * Adds the MockK dependency to the testing set.
 */
fun DependencyHandler.mockk() {
    add("testImplementation", "io.mockk:mockk:$MOCKK_VERSION")
}

/**
 * Adds the JeroMQ dependency.
 */
fun DependencyHandler.jeromq() {
    add("implementation", "org.zeromq:jeromq:$JEROMQ_VERSION")
}

/**
 * Adds logging dependencies, including kotlin logging and slf4j.
 */
fun DependencyHandler.logging() {
    add("implementation", "io.github.microutils:kotlin-logging:$KOTLIN_LOGGING_VERSION")
    add("implementation", "org.slf4j:slf4j-simple:$SLF4J_VERSION")
}
