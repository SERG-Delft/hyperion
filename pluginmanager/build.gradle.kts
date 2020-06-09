plugins {
    kotlinPlugins()
}

setupKotlinPlugins()
setupJacocoPlugin(branchCoverage = 0.8, lineCoverage = 0.8, runOnIntegrationTest = true)

application {
    mainClassName = "nl.tudelft.hyperion.pluginmanager.Main"
}

dependencies {
    // ZeroMQ
    jeromq()
    coroutines()

    // yaml parsing
    jackson()

    // Logging
    logging()

    // Testing
    mockk()
}

detekt {
    config = files("detekt-config.yml")
}
