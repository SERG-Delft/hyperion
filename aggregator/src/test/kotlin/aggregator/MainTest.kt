package aggregator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MainTest() {
    @Test
    fun addTwoTwo() {
        assertEquals(4, add(2, 2))
    }

    @Test
    fun addTwoOne() {
        assertEquals(3, add(2, 1))
    }
}
