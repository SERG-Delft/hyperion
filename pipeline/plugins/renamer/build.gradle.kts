plugins {
    kotlinPlugins()
}

setupKotlinPlugins()
setupJacocoPlugin(branchCoverage = 0.8, lineCoverage = 0.5)

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

application {
    mainClassName = "nl.tudelft.hyperion.pipeline.renamer.Main"
}