plugins {
    kotlin("jvm") version "1.3.71"
}

group = "nl.sapmannen"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}
allprojects {
    repositories {
        mavenCentral()
        jcenter()
    }
}



