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

jacoco {
    toolVersion = "0.8.5"
    reportsDir = file("$buildDir/jacoco")
}

dependencies {
    // Yaml deserialization
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.7.1-1")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.7.1-2")
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.7.1")

    // Web server
    implementation("io.javalin", "javalin", "3.8.0")

    // Database connection
    implementation("org.jetbrains.exposed", "exposed-core", "0.23.1")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.23.1")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.23.1")
    implementation("org.jetbrains.exposed", "exposed-jodatime", "0.23.1")
    implementation("org.postgresql", "postgresql", "42.2.12")
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
    destinationDir = File("./");
}