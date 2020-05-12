package nl.tudelft.hyperion.renamer.main

import nl.tudelft.hyperion.renamer.main
import org.junit.jupiter.api.Test
import java.nio.file.NoSuchFileException
import kotlin.test.assertFailsWith

class MainTest {
    @Test
    fun mainThrows() {
        assertFailsWith<NoSuchFileException>{ main("./notJSON.txt") }
    }
}