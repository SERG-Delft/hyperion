plugins {
    kotlinPlugins()
}

setupKotlinPlugins()
setupJacocoPlugin(branchCoverage = 0.8, lineCoverage = 0.4, runOnIntegrationTest = true)

application {
    mainClassName = "nl.tudelft.hyperion.pipeline.plugins.adder.Main"
}

dependencies {
    // Kotlin class reflection
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.70")

    // json parsing
    jackson(
        withKotlin = false,
        withYAML = false
    )

    // Logging
    logging()

    // testing
    jeromq()
    coroutines()

    // local modules
    implementation(project(":pipeline:common"))
}
