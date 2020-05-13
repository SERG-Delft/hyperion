package nl.tudelft.hyperion.aggregator.utils

import org.junit.jupiter.api.BeforeAll
import java.io.File

/**
 * Utility class that allows all logging statements to continue (thus testing
 * the log implementation), but tunnels all the logs to a temporary file deleted
 * after the test completes.
 */
abstract class TestWithoutLogging {
    companion object {
        @BeforeAll
        @JvmStatic
        fun `Disable logging`() {
            val tmpFile = File.createTempFile("hyperion-test-logs", "log")

            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "TRACE")
            System.setProperty("org.slf4j.simpleLogger.logFile", tmpFile.absolutePath)
        }
    }
}
