plugins {
    application
    jacoco
    kotlin("jvm")
    id("org.jetbrains.intellij") version "0.4.18"
    id("io.gitlab.arturbosch.detekt").version("1.8.0")
    id("com.github.johnrengelman.shadow").version("5.2.0")
}

application {
    mainClassName = "nl.tudelft.hyperion.sampleplugin.Main"
}

dependencies {
    // redis client
    implementation("io.lettuce", "lettuce-core", "5.3.0.RELEASE")

    // yaml parsing
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.9.4")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.10.2")
    implementation("com.fasterxml.jackson.datatype", "jackson-datatype-joda", "2.9.4")
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.9.4")

    // Logging
    implementation("io.github.microutils", "kotlin-logging", "1.7.9")
    implementation("org.slf4j", "slf4j-simple", "1.7.28")

    // local modules
    implementation(project(":pluginmanager"))
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
