plugins {
    application
    jacoco
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt").version("1.8.0")
    id("com.github.johnrengelman.shadow").version("5.2.0")
}

jacoco {
    toolVersion = "0.8.5"
    reportsDir = file("$buildDir/jacoco")
}

dependencies {
    // Necessary for coroutines
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.5")

    // Used for command line parsing
    implementation("com.github.ajalt", "clikt", "2.6.0")

    // ZeroMQ
    implementation("org.zeromq", "jeromq", "0.5.2")

    // Used for testing
    implementation("org.projectlombok", "lombok", "1.18.12")
    annotationProcessor("org.projectlombok", "lombok", "1.18.12")
    testImplementation("org.junit.jupiter", "junit-jupiter-params", "5.6.2")
    testImplementation("io.mockk", "mockk", "1.9.3")
    testImplementation("net.bytebuddy", "byte-buddy", "1.10.10")
    testImplementation("org.jetbrains.kotlinx", "kotlinx-coroutines-test", "1.3.5")
    testImplementation("org.testcontainers", "testcontainers", "1.14.1")
    testImplementation("org.testcontainers", "junit-jupiter", "1.14.1")
    testImplementation("org.testcontainers", "elasticsearch", "1.14.1")

    // Used for logging
    implementation("io.github.microutils", "kotlin-logging", "1.7.9")
    implementation("org.slf4j", "slf4j-simple", "1.7.28")

    // Elasticsearch rest client
    implementation("org.elasticsearch.client", "elasticsearch-rest-high-level-client", "7.6.2")
    implementation("org.apache.logging.log4j", "log4j-core", "2.13.2")

    // Used for YAML deserialization
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.11.0")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.11.0")
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml","2.11.0")

    // Add datasource commons
    implementation(project(":datasource:common"))
}

application {
    mainClassName = "nl.tudelft.hyperion.datasource.plugins.elasticsearch.Main"
}


jacoco {
    toolVersion = "0.8.5"
    reportsDir = file("$buildDir/jacoco")
}

tasks.integrationTest {
    jacoco {
        enabled = true
    }
}

tasks.jacocoTestReport {
    executionData(
            tasks.run.get(),
            tasks.integrationTest.get()
    )

    reports {
        xml.isEnabled = false
        csv.isEnabled = false
        html.destination = file("${buildDir}/jacocoHtml")
    }
}

tasks.jacocoTestCoverageVerification {
    executionData(
            tasks.run.get(),
            tasks.integrationTest.get()
    )

    violationRules {
        rule {
            limit {
                counter = "BRANCH"
                minimum = "0.7".toBigDecimal()
            }

            limit {
                counter = "LINE"
                minimum = "0.8".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.build {
    dependsOn(tasks.jacocoTestReport)
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    destinationDirectory.set(File("./"))
}

detekt {
    config = files("detekt-config.yml")
}