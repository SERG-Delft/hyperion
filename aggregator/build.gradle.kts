plugins {
    application
    jacoco
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt").version("1.8.0")
}

application {
    mainClassName = "aggregator.Main"
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
                minimum = "0.8".toBigDecimal()
            }

            limit {
                counter = "LINE"
                minimum = "0.8".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.build {
    dependsOn(tasks.jacocoTestReport)
}
