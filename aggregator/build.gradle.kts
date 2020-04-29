plugins {
    application
    kotlin("jvm")
}

application {
    mainClassName = "aggregator.Main"
}

dependencies {
    compile(kotlin("stdlib"))
}