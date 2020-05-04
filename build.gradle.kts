plugins {
    kotlin("jvm") version "1.3.71"
}

group = "nl.tudelft.hyperion"
version = "1.0-SNAPSHOT"

sourceSets {
    create("systemTest")
}

configurations["systemTestImplementation"].extendsFrom(configurations.testImplementation.get())
configurations["systemTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

val systemTest = task<Test>("systemTest") {
    description = "Runs system tests"
    group = "verification"

    useJUnitPlatform()

    testClassesDirs = sourceSets["systemTest"].output.classesDirs
    classpath = sourceSets["systemTest"].runtimeClasspath
    shouldRunAfter("test")
}

tasks.check {
    dependsOn(systemTest)
}

allprojects {
    apply(plugin = "kotlin")

    repositories {
        mavenCentral()
        jcenter()
    }

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
    }

    tasks {
        compileKotlin {
            kotlinOptions.jvmTarget = "11"
        }
        compileTestKotlin {
            kotlinOptions.jvmTarget = "11"
        }
    }

    tasks.test {
        useJUnitPlatform()
        failFast = true
        reports.html.isEnabled = false
        reports.junitXml.isEnabled = false
    }
}

subprojects {
    apply(plugin= "kotlin")

    sourceSets {
        create("integrationTest") {
            compileClasspath += project.files("src/integrationTest/")
            runtimeClasspath += project.files("src/integrationTest/")
        }
    }

    configurations["integrationTestImplementation"].extendsFrom(configurations.testImplementation.get())
    configurations["integrationTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

    val integrationTest = task<Test>("integrationTest") {
        description = "Runs integration tests"
        group = "verification"

        useJUnitPlatform()

        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        shouldRunAfter("test")
    }

    tasks.check {
        dependsOn(integrationTest)
    }
}
