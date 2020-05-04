plugins {
    application
    kotlin("jvm")
}

dependencies {
    implementation("org.elasticsearch.client:elasticsearch-rest-high-level-client:7.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
    implementation("io.github.microutils:kotlin-logging:1.7.9")

    // necessary for the elasticsearch rest client
    implementation("org.apache.logging.log4j:log4j-core:2.13.2")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.13.2")

    // add commons
    implementation(project("::datasource:common"))
}

application {
    mainClassName = "nl.tudelft.hyperion.datasource.plugins.elasticsearch.Main"
}