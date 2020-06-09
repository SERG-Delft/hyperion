plugins {
    kotlinPlugins()
}

setupKotlinPlugins()
setupJacocoPlugin(branchCoverage = 0.6, lineCoverage = 0.7, runOnIntegrationTest = true)

dependencies {
    // Coroutines
    coroutines()

    // Logging
    logging()

    // Yaml/JSON deserialization
    jackson(
        withKotlin = false,
        withYAML = false
    )

    // ZeroMQ
    jeromq()

    // Used for testing
    testImplementation("org.jetbrains.kotlinx", "kotlinx-coroutines-test", "1.3.5")
    testImplementation("net.bytebuddy", "byte-buddy", "1.10.10")
    mockk()

    // Add pipeline commons
    implementation(project(":pipeline:common"))
}

application {
    mainClassName = "nl.tudelft.hyperion.pipeline.loadbalancer.MainKt"
}
