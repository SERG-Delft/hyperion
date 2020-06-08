plugins {
    kotlinPlugins()
}

setupKotlinPlugins()
setupJacocoPlugin(branchCoverage = 0.4, lineCoverage = 0.7, runOnIntegrationTest = true)

dependencies {
    // Yaml/JSON deserialization
    jackson()

    // Logging
    logging()

    // ZeroMQ
    jeromq()
    coroutines()

    // Testing
    mockk()
}

detekt {
    config = files("detekt-config.yml")
}