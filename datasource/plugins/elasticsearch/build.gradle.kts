plugins {
    kotlinPlugins()
}

setupKotlinPlugins()
setupJacocoPlugin(branchCoverage = 0.7, lineCoverage = 0.8, runOnIntegrationTest = true)

dependencies {
    // Necessary for coroutines
    coroutines()

    // Used for command line parsing
    implementation("com.github.ajalt", "clikt", "2.6.0")

    // Pipeline
    implementation(project(":pipeline:common"))

    // Used for testing
    implementation("org.projectlombok", "lombok", "1.18.12")
    annotationProcessor("org.projectlombok", "lombok", "1.18.12")
    testImplementation("org.junit.jupiter", "junit-jupiter-params", "5.6.2")
    mockk()
    testImplementation("net.bytebuddy", "byte-buddy", "1.10.10")
    testImplementation("org.jetbrains.kotlinx", "kotlinx-coroutines-test", "1.3.5")
    testImplementation("org.testcontainers", "testcontainers", "1.14.1")
    testImplementation("org.testcontainers", "junit-jupiter", "1.14.1")
    testImplementation("org.testcontainers", "elasticsearch", "1.14.1")
    testImplementation("org.zeromq", "jeromq", "0.5.2")

    // Used for logging
    logging()

    // Elasticsearch rest client
    implementation("org.elasticsearch.client", "elasticsearch-rest-high-level-client", "7.6.2")
    implementation("org.apache.logging.log4j", "log4j-core", "2.13.2")

    // Used for YAML deserialization
    jackson()

    // Add datasource commons
    implementation(project(":datasource:common"))
}

application {
    mainClassName = "nl.tudelft.hyperion.datasource.plugins.elasticsearch.Main"
}

detekt {
    config = files("detekt-config.yml")
}
