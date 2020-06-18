import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
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
    apply(plugin = "com.bmuschko.docker-remote-api")

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

// val releaseArtifacts = listOf(":pluginmanager:", ":datasource:plugins:elasticsearch:", ":aggregator:",
//                               ":pipeline:plugins:adder:", ":pipeline:plugins:extractor:",
//                               ":pipeline:plugins:loadbalancer:", ":pipeline:plugins:pathextractor:",
//                               ":pipeline:plugins:printer:", ":pipeline:plugins:rate:",
//                               ":pipeline:plugins:reader:", ":pipeline:plugins:renamer:",
//                               ":pipeline:plugins:stresser:", ":pipeline:plugins:versiontracker:")

val releaseArtifacts = listOf(":pipeline:plugins:adder:")

tasks.register<DockerBuildImage>("docker-image") {
    group = "docker"
    description = "Create docker image"
    doFirst {
        println("building image")
    }
}

tasks.register<DockerPushImage>("docker-push") {
    group = "docker"
    description = "Pushed a docker image to docker hub"
    doFirst {
        println("pushing image")
    }
}

configure(subprojects.filter { releaseArtifacts.contains<String>(it.path + ":")}) {
    version = (if (project.properties["version"] != "unspecified") { project.properties["version"] } else { "0.1.0" })!!
    val dockerId = "daveter9"
    // format the artifact name in the necessary formats
    val artifact = project.path + ":"
    val artifactParts = artifact.split(":").drop(1).dropLast(1)
    val artifactName = "hyperion-${artifactParts.joinToString("-", "", "", limit = -1)}:$version"

    val imageName = "$dockerId/$artifactName"

    tasks.register<DockerBuildImage>("docker-image") {
        dependsOn(artifact + "shadowJar")

        docker {
            url.set(project.properties["dockerHost"].toString())
            //dockerFile.set(dockerfileArtifactPath)
            inputDir.set(File("."))
            imageId.set(artifactName)
            images.add(imageName)
            registryCredentials {
                username.set(project.properties["dockerUsername"].toString())
                password.set(project.properties["dockerPassword"].toString())
                email.set(project.properties["dockerEmail"].toString())
            }
        }
    }

    tasks.register<DockerPushImage>("docker-push") {
        dependsOn(artifact + "docker-image")
        //println("docker image $project")
        docker {
            url.set(project.properties["dockerHost"].toString())
            registryCredentials {
                username.set(project.properties["dockerUsername"].toString())
                password.set(project.properties["dockerPassword"].toString())
                email.set(project.properties["dockerEmail"].toString())
            }
            images.add(imageName)
        }
    }
}

tasks.register<DefaultTask>("docker-release") {
    group = "docker"
    description = "Release all artifacts as docker image on docker hub"
    for (artifact in releaseArtifacts) {
        dependsOn(artifact + "docker-push")
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
