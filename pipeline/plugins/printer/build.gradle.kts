plugins {
    kotlinPlugins()
}

setupKotlinPlugins()
setupJacocoPlugin(branchCoverage = 0.5, lineCoverage = 0.1)

application {
    mainClassName = "nl.tudelft.hyperion.pipeline.plugins.printer.Main"
}

dependencies {
    // testing
    coroutines()

    // Logging
    logging()

    implementation(project(":pipeline:common"))
}
