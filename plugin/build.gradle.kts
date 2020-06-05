plugins {
    application
    jacoco
    kotlin("jvm")
    id("org.jetbrains.intellij").version("0.4.21")
    id("io.gitlab.arturbosch.detekt").version("1.8.0")
    id("com.github.johnrengelman.shadow").version("5.2.0")
}

intellij {
    setPlugins("git4idea")
}

dependencies {
    // To establish a connection and make a get request to the API.
    implementation("io.ktor:ktor-client-core:1.3.2")
    // CIO is the HttpClient we use for the connection.
    implementation("io.ktor:ktor-client-cio:1.3.2")

    // To deserialize incoming JSON from the API.
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.10.2")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.10.2")

    // To easily format different intervals when displaying Metrics.
    implementation("joda-time", "joda-time", "2.10.6")

    testImplementation("org.junit.jupiter", "junit-jupiter-params", "5.6.2")
    testImplementation("io.mockk", "mockk", "1.9.3")

    // Used for testing requests by ktor.
    testImplementation("io.ktor:ktor-client-mock:1.3.2")
    testImplementation("io.ktor:ktor-client-mock-jvm:1.3.2")
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
                minimum = "0.3".toBigDecimal()
            }

            limit {
                counter = "LINE"
                minimum = "0.4".toBigDecimal()
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

detekt {
    config = files("detekt-config.yml")
}
