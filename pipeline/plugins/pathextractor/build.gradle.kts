plugins {
    application
    jacoco
    kotlin("jvm")
    id("org.jetbrains.intellij") version "0.4.18"
    id("io.gitlab.arturbosch.detekt").version("1.8.0")
    id("com.github.johnrengelman.shadow").version("5.2.0")
}

application {
    mainClassName = "nl.tudelft.hyperion.pipeline.pathextractor.Main"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.70")

    // Testing
    testImplementation("io.mockk", "mockk", "1.10.0")

    implementation(project(":pipeline:common"))
}

dependencies {
    implementation("io.ktor:ktor-client-core:1.3.2")
    implementation("io.ktor:ktor-client-cio:1.3.2")
    implementation("joda-time", "joda-time", "2.10.6")
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
    destinationDir = File("./")
}