plugins {
    application
    jacoco
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt").version("1.8.0")
    id("com.github.johnrengelman.shadow").version("5.2.0")
}

application {
    mainClassName = "nl.tudelft.hyperion.aggregator.Main"
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.8.0")
}

jacoco {
    toolVersion = "0.8.5"
    reportsDir = file("$buildDir/jacoco")
}

dependencies {
    // Yaml/JSON deserialization
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.9.4")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.9.4")
    implementation("com.fasterxml.jackson.datatype", "jackson-datatype-joda", "2.9.4")
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.9.4")

    // Web server
    implementation("io.javalin", "javalin", "3.8.0")

    // Database connection
    implementation("org.jetbrains.exposed", "exposed-core", "0.23.1")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.23.1")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.23.1")
    implementation("org.jetbrains.exposed", "exposed-jodatime", "0.23.1")
    implementation("org.postgresql", "postgresql", "42.2.12")

    // Intake
    implementation("io.lettuce", "lettuce-core", "5.3.0.RELEASE")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.3.5")

    // Logging
    implementation("io.github.microutils", "kotlin-logging", "1.7.9")
    implementation("org.slf4j", "slf4j-simple", "1.7.28")

    // Testing
    testImplementation("io.mockk", "mockk", "1.10.0")
    testImplementation("org.xerial", "sqlite-jdbc", "3.31.1")
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
                minimum = "0.8".toBigDecimal()
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
    destinationDir = File("./")
}

detekt {
    config = files("detekt-config.yml")
}
