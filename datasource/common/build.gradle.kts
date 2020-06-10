plugins {
    kotlinPlugins()
}

setupKotlinPlugins()
setupJacocoPlugin(branchCoverage = 0.8, lineCoverage = 0.8)

dependencies {
    coroutines()
}
