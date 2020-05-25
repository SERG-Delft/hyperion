import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
    kotlin("jvm") version "1.3.71"
}

group = "nl.tudelft.hyperion"
version = "1.0-SNAPSHOT"

sourceSets {
    create("systemTest")
}

sourceSets.main {
    java.srcDirs("src/main/java", "src/main/kotlin")
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

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    tasks.test {
        useJUnitPlatform()
        failFast = true
        reports.html.isEnabled = false
        reports.junitXml.isEnabled = false
    }
}

subprojects {
    apply(plugin = "kotlin")

    sourceSets {
        create("integrationTest") {
            compileClasspath += sourceSets.main.get().output
            compileClasspath += project.files("src/integrationTest/")
            runtimeClasspath += sourceSets.main.get().output
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
