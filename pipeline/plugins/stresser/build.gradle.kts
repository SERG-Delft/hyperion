plugins {
    kotlinPlugins()
}

setupKotlinPlugins()
setupJacocoPlugin(branchCoverage = 0.5, lineCoverage = 0.4)

application {
    mainClassName = "nl.tudelft.hyperion.pipeline.plugins.stresser.Main"
}

dependencies {
    // testing
    coroutines()
    mockk()

    // Logging
    logging()

    implementation(project(":pipeline:common"))
}
