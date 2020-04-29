plugins {
    application
    kotlin("jvm")
    id("org.jetbrains.intellij") version "0.4.18"
}

application {
    mainClassName = "plugin.Main"
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("junit:junit:4.12")
}