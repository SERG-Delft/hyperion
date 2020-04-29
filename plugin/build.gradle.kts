plugins {
    application
    jacoco
    kotlin("jvm")
    id("org.jetbrains.intellij") version "0.4.18"
    id("io.gitlab.arturbosch.detekt").version("1.8.0")
}

application {
    mainClassName = "plugin.Main"
}

jacoco {
    toolVersion = "0.8.5"
    reportsDir = file("$buildDir/jacoco")
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = false
        csv.isEnabled = false
        html.destination = file("${buildDir}/jacocoHtml")
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "BRANCH"
                value = "COVEREDRATIO"
                minimum = "0.8".toBigDecimal()
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
