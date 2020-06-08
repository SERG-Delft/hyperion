plugins {
    kotlinPlugins()
}

setupKotlinPlugins()
setupJacocoPlugin(branchCoverage = 0.0, lineCoverage = 0.5, runOnIntegrationTest = true)

application {
    mainClassName = "nl.tudelft.hyperion.pipeline.plugins.rate.Main"
}

dependencies {
    // Kotlin class reflection
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.70")

    // testing
    mockk()

    // ZeroMQ
    jeromq()
    coroutines()

    // Logging
    logging()

    // local modules
    implementation(project(":pipeline:common"))
}
