import java.util.Date

plugins {
    kotlinPlugins()
    id("maven-publish")
    id("org.jetbrains.dokka") version "0.10.1"
    signing
}

group = "com.github.sergdelft.hyperion"
val pubGroup = group.toString()
version = "0.1.0"

setupKotlinPlugins()
setupJacocoPlugin(branchCoverage = 0.4, lineCoverage = 0.7, runOnIntegrationTest = true)

dependencies {
    // Yaml/JSON deserialization
    jackson()

    // Logging
    logging()

    // ZeroMQ
    jeromq()
    coroutines()

    // Testing
    mockk()
}

detekt {
    config = files("detekt-config.yml")
}

tasks.dokka {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

val dokkaJar by tasks.creating(Jar::class) {
    description = "Assembles Kotlin docs with Dokka"
    classifier = "javadoc"
    from(tasks.dokka)
}



publishing {
    publications {
        create<MavenPublication>("pipeline-common") {
            artifactId = "pipeline-common"
            groupId = pubGroup
            version = "0.1.0"
            from(components["java"])

            // include sources jar
            artifact(sourcesJar)

            // include kdoc jar
            artifact(dokkaJar)

            pom {
                name.set("$pubGroup:pipeline-common")
                description.set("Easily write pipeline plugins for the Hyperion logging framework.")
                url.set("https://github.com/SERG-Delft/monitoring-aware-ides")


                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("Hyperion authors")
                    }
                }

                scm {
                    url.set("https://github.com/SERG-Delft/monitoring-aware-ides")
                }
            }
        }
    }

    repositories {
        maven {
            // update this url to correct repo
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            credentials {
                // Here is the previous account registered in issues. sonatype. org.
                username = project.properties["sonatypeUsername"].toString()
                password = project.properties["sonatypePassword"].toString()
            }
        }
    }
}

// bintray {
//     user = project.findProperty("bintrayUser").toString()
//     key = project.findProperty("bintrayKey").toString()
//     publish = true
//     setPublications("pipeline-common")
//
//     pkg.apply {
//         repo = "monitoring-aware-ides"
//         name = "$pubGroup:pipeline-common"
//         setLicenses("Apache-2.0")
//         userOrg = "serg-tudelft"
//         vcsUrl = "https://github.com/SERG-Delft/monitoring-aware-ides.git"
//         version.apply {
//             name = "0.1.0"
//             desc = "0.1.0"
//             released = Date().toString()
//         }
//     }
// }

signing {
    sign(publishing.publications["pipeline-common"])
}