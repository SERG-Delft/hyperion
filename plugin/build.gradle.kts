plugins {
    application
    id("org.jetbrains.intellij") version "0.4.18"
    kotlin("jvm")
}

application {
    mainClassName = "plugin.Main"
}

dependencies {
    compile(kotlin("stdlib"))
}