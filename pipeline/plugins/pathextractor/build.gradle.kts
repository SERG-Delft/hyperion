plugins {
    kotlinPlugins()
}

setupKotlinPlugins()
setupJacocoPlugin(branchCoverage = 0.5, lineCoverage = 0.5)

application {
    mainClassName = "nl.tudelft.hyperion.pipeline.pathextractor.Main"
}

dependencies {
    // Kotlin class reflection
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.70")

    implementation(project(":pipeline:common"))

    // JSON deserialization & serialization
    jackson(
        withKotlin = false,
        withYAML = false
    )
}
