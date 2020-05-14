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
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.5")
    implementation("com.github.ajalt", "clikt", "2.6.0")
    implementation("io.lettuce", "lettuce-core", "5.3.0.RELEASE")

    // ZeroMQ
    implementation("org.zeromq", "jeromq", "0.5.2")

    // Used for testing
    testImplementation("org.junit.jupiter", "junit-jupiter-params", "5.6.2")
    testImplementation("io.mockk", "mockk", "1.9.3")
    testImplementation("net.bytebuddy", "byte-buddy", "1.10.10")
    testImplementation("org.testcontainers", "testcontainers", "1.14.1")
    testImplementation("org.testcontainers", "junit-jupiter", "1.14.1")

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

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = false
        csv.isEnabled = false
        html.destination = file("${buildDir}/jacocoHtml")
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "BRANCH"
                minimum = "0.65".toBigDecimal()
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