plugins {
    application
    kotlin("jvm")
    id("com.github.johnrengelman.shadow").version("5.2.0")
}

dependencies {
    implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
    implementation("com.github.ajalt:clikt:2.6.0")

    // necessary for the elasticsearch rest client
    implementation("org.apache.logging.log4j:log4j-core:2.13.2")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.13.2")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.7.1-1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.7.1-2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.7.1")

    // add commons
    implementation(project("::datasource:common"))
}

application {
    mainClassName = "nl.tudelft.hyperion.datasource.plugins.elasticsearch.Main"
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    destinationDir = File("./");
}