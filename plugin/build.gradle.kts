plugins {
    application
    kotlin("jvm")
    id("org.jetbrains.intellij") version "0.4.18"
}

application {
    mainClassName = "plugin.Main"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
}