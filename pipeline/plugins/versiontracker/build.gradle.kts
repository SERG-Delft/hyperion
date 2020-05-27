plugins {
    application
    jacoco
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt").version("1.8.0")
    id("com.github.johnrengelman.shadow").version("5.2.0")
}

dependencies {
    // For git
    implementation("org.eclipse.jgit", "org.eclipse.jgit", "5.7.0.202003110725-r")

    // Necessary for coroutines
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.5")

    // Yaml/JSON deserialization
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.10.2")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.10.2")
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.10.2")

    // Used for testing
    testImplementation("org.junit.jupiter", "junit-jupiter-params", "5.6.2")
    testImplementation("io.mockk", "mockk", "1.9.3")
    testImplementation("net.bytebuddy", "byte-buddy", "1.10.10")

    // Logging
    implementation("io.github.microutils", "kotlin-logging", "1.7.9")
    implementation("org.slf4j", "slf4j-simple", "1.7.28")

    implementation(project(":pipeline:common"))
}

application {
    mainClassName = "nl.tudelft.hyperion.pipeline.versiontracker.MainKt"
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
                minimum = "0.7".toBigDecimal()
            }

            limit {
                counter = "LINE"
                minimum = "0.75".toBigDecimal()
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
