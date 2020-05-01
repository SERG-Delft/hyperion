package plugin

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class MainTest() {
    @Test
    fun addTwoTwo() {
        Assertions.assertEquals(4, add(2, 2))
    }

    @Test
    fun addTwoOne() {
        Assertions.assertEquals(3, add(2, 1))
    }

    @Test
    fun testMain() {
        main("test")
    }
}