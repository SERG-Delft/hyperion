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
    implementation("org.elasticsearch.client", "elasticsearch-rest-high-level-client", "7.6.2")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.5")
    implementation("com.github.ajalt", "clikt", "2.6.0")
    implementation("redis.clients", "jedis", "3.2.0")

    // Used for logging
    implementation("io.github.microutils", "kotlin-logging", "1.7.9")
    implementation("org.slf4j", "slf4j-simple", "1.7.28")

    // Necessary for the elasticsearch rest client
    implementation("org.apache.logging.log4j", "log4j-core", "2.13.2")
    implementation("org.apache.logging.log4j", "log4j-slf4j-impl", "2.13.2")

    // Used for YAML deserialization
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.7.1-1")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.7.1-2")
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml","2.7.1")

    // Add datasource commons
    implementation(project("::datasource:common"))
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
    destinationDirectory.set(File("./"))
}

detekt {
    config = files("detekt-config.yml")
}