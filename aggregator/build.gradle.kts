plugins {
    kotlinPlugins()
}

application {
    mainClassName = "nl.tudelft.hyperion.aggregator.Main"
}

setupKotlinPlugins()
setupJacocoPlugin(branchCoverage = 0.7, lineCoverage = 0.8, runOnIntegrationTest = true)

dependencies {
    // Yaml/JSON deserialization
    jackson(
        withJoda = true
    )

    // Web server
    implementation("io.javalin", "javalin", "3.8.0")

    // Database connection
    implementation("org.jetbrains.exposed", "exposed-core", "0.23.1")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.23.1")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.23.1")
    implementation("org.jetbrains.exposed", "exposed-jodatime", "0.23.1")
    implementation("org.postgresql", "postgresql", "42.2.12")

    // Intake
    coroutines()
    implementation(project(":pipeline:common"))

    // Logging
    logging()

    // Testing
    mockk()
    jeromq()
    testImplementation("org.xerial", "sqlite-jdbc", "3.31.1")
    testImplementation("org.junit.jupiter", "junit-jupiter-params", "5.6.2")

    // Integration test
    testImplementation("org.testcontainers", "testcontainers", "1.14.1")
    testImplementation("org.testcontainers", "postgresql", "1.14.1")
    testImplementation("org.testcontainers", "junit-jupiter", "1.14.1")
}

detekt {
    config = files("detekt-config.yml")
}
