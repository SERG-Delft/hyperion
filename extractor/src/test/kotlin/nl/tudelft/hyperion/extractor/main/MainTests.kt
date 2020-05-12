package nl.tudelft.hyperion.extractor.main

import nl.tudelft.hyperion.extractor.main
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import java.nio.file.NoSuchFileException

class MainTests() {
    @Test
    fun testSimpleMessage() {
        assertFailsWith<NoSuchFileException> { main("./noJSON.txt") }
    }
}
