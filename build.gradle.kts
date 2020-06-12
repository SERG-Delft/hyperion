import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
plugins {
    kotlin("jvm") version "1.3.71"
    id("com.bmuschko.docker-remote-api") version "6.4.0"
}

group = "nl.tudelft.hyperion"
version = "0.1.0"

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

val releaseArtifacts = listOf(":pluginmanager:", ":datasource:plugins:elasticsearch:", ":aggregator:",
                              ":pipeline:plugins:adder:", ":pipeline:plugins:extractor:",
                              ":pipeline:plugins:loadbalancer:", ":pipeline:plugins:pathextractor:",
                              ":pipeline:plugins:printer:", ":pipeline:plugins:rate:",
                              ":pipeline:plugins:reader:", ":pipeline:plugins:renamer:",
                              ":pipeline:plugins:stresser:", ":pipeline:plugins:versiontracker:")


tasks.register<DockerPushImage>("docker-release") {
    group = "docker"
    description = "Release all artifacts as docker image on docker hub"
    val dockerUrl = "hub.docker.com/username/"
    val version = "0.1.0"

    //dependsOn("build-artifacts-release")

    docker {
        registryCredentials  {
            url.set("")
        }
    }

    for (artifact in releaseArtifacts) {
        val artifactParts = artifact.split(":")
        val artifactName =  artifactParts[artifactParts.size - 2]

        val dockerArtifactUrl = "$dockerUrl$artifactName:$version"
        val dockerArtifactPath = File("/release-artifacts/$artifactName-all.jar")
        val dockerfileArtifactPath = File(artifactParts.joinToString("/", limit= -1) + "Dockerfile")
    }

}

tasks.register<DefaultTask>("build-artifacts-release") {
    group = "build"
    description = "Produces all necessary artifacts for a GitHub Release"

    // Ensure that shadow outputs to one directory.
    allprojects {
        try {
            tasks.getByName<AbstractArchiveTask>("shadowJar") {
                destinationDirectory.set(File(project.rootDir.absolutePath + "/release-artifacts/"))
            }
        } catch (ex: UnknownDomainObjectException) {

        }
    }

    for (artifact in releaseArtifacts) {
        dependsOn(artifact + "shadowJar")
    }
}
