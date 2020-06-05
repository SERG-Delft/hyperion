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
    // Coroutines
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.5")

    // Logging
    implementation("io.github.microutils", "kotlin-logging", "1.7.9")
    implementation("org.slf4j", "slf4j-simple", "1.7.28")

    // Yaml/JSON deserialization
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.9.4")

    // ZeroMQ
    implementation("org.zeromq", "jeromq", "0.5.2")

    // Used for testing
    testImplementation("org.jetbrains.kotlinx", "kotlinx-coroutines-test", "1.3.5")
    testImplementation("net.bytebuddy", "byte-buddy", "1.10.10")
    testImplementation("io.mockk", "mockk", "1.9.3")

    // Add pipeline commons
    implementation(project(":pipeline:common"))
}

application {
    mainClassName = "nl.tudelft.hyperion.pipeline.loadbalancer.MainKt"
}

jacoco {
    toolVersion = "0.8.5"
    reportsDir = file("$buildDir/jacoco")
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
                minimum = "0.6".toBigDecimal()
            }

            limit {
                counter = "LINE"
                minimum = "0.7".toBigDecimal()
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
    destinationDirectory.set(File("./build"))
}
