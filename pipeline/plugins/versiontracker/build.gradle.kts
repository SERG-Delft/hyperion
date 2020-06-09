plugins {
    kotlinPlugins()
}

setupKotlinPlugins()
setupJacocoPlugin(branchCoverage = 0.7, lineCoverage = 0.75)

dependencies {
    // For git
    implementation("org.eclipse.jgit", "org.eclipse.jgit", "5.7.0.202003110725-r")

    // Necessary for coroutines
    coroutines()

    // Yaml/JSON deserialization
    jackson()

    // Used for testing
    testImplementation("org.junit.jupiter", "junit-jupiter-params", "5.6.2")
    mockk()
    testImplementation("net.bytebuddy", "byte-buddy", "1.10.10")

    // Logging
    logging()

    implementation(project(":pipeline:common"))
}

application {
    mainClassName = "nl.tudelft.hyperion.pipeline.versiontracker.MainKt"
}
