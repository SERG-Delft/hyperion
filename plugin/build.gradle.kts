plugins {
    kotlinPlugins()
    id("org.jetbrains.intellij").version("0.4.21")
}

setupKotlinPlugins()
setupJacocoPlugin(branchCoverage = 0.3, lineCoverage = 0.4)

intellij {
    setPlugins("git4idea")
    version = "2020.1"
}
tasks.patchPluginXml {
    sinceBuild("201.4163")
}


dependencies {
    // To establish a connection and make a get request to the API.
    implementation("io.ktor:ktor-client-core:1.3.2")
    // CIO is the HttpClient we use for the connection.
    implementation("io.ktor:ktor-client-cio:1.3.2")

    // To deserialize incoming JSON from the API.
    jackson(withYAML = false)

    // To easily format different intervals when displaying Metrics.
    implementation("joda-time", "joda-time", "2.10.6")

    mockk()

    // Used for testing requests by ktor.
    testImplementation("io.ktor:ktor-client-mock:1.3.2")
    testImplementation("io.ktor:ktor-client-mock-jvm:1.3.2")

    // For ParameterizedTests.
    testImplementation("org.junit.jupiter", "junit-jupiter-params", "5.6.2")
}


detekt {
    config = files("detekt-config.yml")
}
