
// This file contains various configurable properties that should be shared
// across all Hyperion sub-projects. They can be referenced from every build
// script. Functions should be in Setup.kt instead.

// Version of Jackson used for (de)serialization.
const val JACKSON_VERSION = "2.9.4"

// Version of JeroMQ used for networking.
const val JEROMQ_VERSION = "0.5.2"

// Kotlin JDK8 coroutines version.
const val KOTLIN_COROUTINES_VERSION = "1.3.5"

// Detekt version used for linting.
const val DETEKT_VERSION = "1.8.0"

const val KOTLIN_LOGGING_VERSION = "1.7.9"
const val SLF4J_VERSION = "1.7.28"
const val MOCKK_VERSION = "1.10.0"

// When changing these, please update the build.gradle.kts in buildSrc accordingly.
const val SHADOW_VERSION = "5.2.0"
const val JACOCO_VERSION = "0.8.5"
