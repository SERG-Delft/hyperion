import org.gradle.api.Project
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.application
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.jacoco
import org.gradle.kotlin.dsl.kotlin
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.File

/**
 * Configures Detekt, Shadow and Jacoco plugins for the current module.
 */
fun PluginDependenciesSpec.kotlinPlugins() {
    application
    jacoco
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt").version(DETEKT_VERSION)
    id("com.github.johnrengelman.shadow").version(SHADOW_VERSION)
}

/**
 * Configures the plugins imported by [kotlinPlugins].
 */
fun Project.setupKotlinPlugins() {
    // Add detekt formatting
    dependencies {
        add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:$DETEKT_VERSION")
    }

    // Ensure that builds depend on shadowing.
    tasks.getByName("build") {
        dependsOn(tasks.getByName("shadowJar"))
    }

    // Ensure that shadow outputs to build.
    tasks.getByName<AbstractArchiveTask>("shadowJar") {
        destinationDirectory.set(File("./build"))
    }
}

/**
 * Configures Jacoco for the current project, enabling it to run on both
 * normal tests and integration tests, and to fail the build if the specified
 * branch or line percentages are not reached.
 */
fun Project.setupJacocoPlugin(branchCoverage: Double, lineCoverage: Double, runOnIntegrationTest: Boolean = false) {
    // Configure jacoco to report to /jacoco.
    extensions.getByName<JacocoPluginExtension>("jacoco").apply {
        toolVersion = JACOCO_VERSION
        reportsDir = file("$buildDir/jacoco")
    }

    // Enable jacoco for integration tests.
    if (runOnIntegrationTest) {
        tasks.getByName("integrationTest") {
            extensions.configure<JacocoTaskExtension>("jacoco") {
                enabled = true
            }
        }
    }

    // Configure reporting.
    tasks.getByName<JacocoReport>("jacocoTestReport") {
        if (runOnIntegrationTest) {
            executionData(
                tasks.getByName("run"),
                tasks.getByName("integrationTest")
            )
        }

        reports {
            xml.isEnabled = false
            csv.isEnabled = false
            html.destination = file("${buildDir}/jacocoHtml")
        }
    }

    // Configure failing when below specified coverage.
    tasks.getByName<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        if (runOnIntegrationTest) {
            executionData(
                tasks.getByName("run"),
                tasks.getByName("integrationTest")
            )
        }

        violationRules {
            rule {
                limit {
                    counter = "BRANCH"
                    minimum = branchCoverage.toBigDecimal()
                }

                limit {
                    counter = "LINE"
                    minimum = lineCoverage.toBigDecimal()
                }
            }
        }
    }

    // Verify coverage when running check.
    tasks.getByName("check") {
        dependsOn(tasks.getByName("jacocoTestCoverageVerification"))
    }

    // Create test report when building.
    tasks.getByName("build") {
        dependsOn(tasks.getByName("jacocoTestReport"))
    }
}
