plugins {
    kotlinPlugins()
}

setupKotlinPlugins()
setupJacocoPlugin(branchCoverage = 0.6, lineCoverage = 0.6)

application {
    mainClassName = "nl.tudelft.hyperion.pipeline.extractor.Main"
}

dependencies {
    // Kotlin class reflection
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.70")

    // Logging
    logging()

    // Local imports
    implementation(project(":pipeline:common"))

    // JSON deserialization & serialization
    jackson(
        withKotlin = false,
        withYAML = false
    )
}
