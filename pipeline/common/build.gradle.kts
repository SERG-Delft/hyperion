import java.util.Date

plugins {
    jacoco
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt").version("1.8.0")
    id("com.github.johnrengelman.shadow").version("5.2.0")
    id("maven-publish")
    id("com.jfrog.bintray") version "1.8.5"
}

jacoco {
    toolVersion = "0.8.5"
    reportsDir = file("$buildDir/jacoco")
}

dependencies {
    // Yaml/JSON deserialization
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.9.4")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.10.2")
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.9.4")

    // Logging
    implementation("io.github.microutils", "kotlin-logging", "1.7.9")
    implementation("org.slf4j", "slf4j-simple", "1.7.28")

    // ZeroMQ
    implementation("org.zeromq", "jeromq", "0.5.2")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.3.5")

    // Testing
    testImplementation("io.mockk", "mockk", "1.10.0")
}

tasks.jacocoTestReport {
    executionData(
        tasks.test.get(),
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
        tasks.test.get(),
        tasks.integrationTest.get()
    )

    violationRules {
        rule {
            limit {
                counter = "BRANCH"
                minimum = "0.5".toBigDecimal()
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

tasks.integrationTest {
    jacoco {
        enabled = true
    }
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

publishing {
    publications {
        create<MavenPublication>("pipeline-common") {
            artifactId = "pipeline-common"
            from(components["java"])

            // include sources jar
            //artifact(sourcesJar)

            pom {
                name.set("nl.tudelft.hyperion:pipeline-common")
                description.set("Easily write pipeline plugins for the Hyperion logging framework.")
                url.set("https://github.com/SERG-Delft/monitoring-aware-ides")


                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("Hyperion authors")
                    }
                }

                scm {
                    url.set("https://github.com/SERG-Delft/monitoring-aware-ides")
                }
            }
        }
    }
}

bintray {
    user = project.findProperty("bintrayUser").toString()
    key = project.findProperty("bintrayKey").toString()
    publish = true
    setPublications("pipeline-common")

    pkg.apply {
        repo = "monitoring-aware-ides"
        name = "nl.tudelft.hyperion:pipeline-common"
        setLicenses("Apache-2.0")
        userOrg = "serg-tudelft"
        vcsUrl = "https://github.com/SERG-Delft/monitoring-aware-ides.git"
        version.apply {
            name = "0.1.0"
            desc = "0.1.0"
            released = Date().toString()
        }
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}