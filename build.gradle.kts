plugins {
    kotlin("jvm") version "1.3.71"
}

group = "nl.sapmannen"
version = "1.0-SNAPSHOT"



allprojects {
    apply(plugin= "kotlin")

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
    }
}

