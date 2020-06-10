package nl.tudelft.hyperion.plugin.settings.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.lang.reflect.Method

/**
 * Test class that tests both IntervalTable class and the IntervalTableModel class.
 */
class IntervalTableTest {
    private val data = listOf(
        Row.parse(3600),
        Row.parse(50),
        Row.parse(420999)
    )
    private lateinit var intervalTable: IntervalTable

    @BeforeEach
    fun setup() {
        intervalTable = IntervalTable(data)
    }

    @Test
    fun `Test IntervalTable constructor and various methods`() {
        assertEquals(data, intervalTable.currentData)
        assertFalse(intervalTable.isModified)

        intervalTable.intervalTableModel.setValueAt(20, 0, 0)

        // The data in the table should now be modified and thus not equal our original data.
        assertNotEquals(data, intervalTable.currentData)
        assertTrue(intervalTable.isModified)

        // updateData() should be equal to the currenData from the TableModel.
        assertEquals(intervalTable.currentData, intervalTable.updateData())
        // Now that we updated our data isModified() should return false.
        assertFalse(intervalTable.isModified)
    }

    @Test
    fun `Test IntervalTable#cloneData() method`() {
        val cloneDataMethod: Method = IntervalTable::class.java.getDeclaredMethod("cloneData", List::class.java)
        assertTrue(cloneDataMethod.trySetAccessible())

        val clone: List<Row> = cloneDataMethod.invoke(intervalTable, data) as List<Row>

        assertEquals(data, clone)

        clone.first().setColumn(0, -1)

        assertNotEquals(data, clone)
    }

    @Test
    fun `Test IntervalTable#reset() method`() {
        intervalTable.intervalTableModel.setValueAt(20, 0, 0)

        assertNotEquals(data, intervalTable.currentData)

        intervalTable.reset()

        assertEquals(data, intervalTable.currentData)
    }

    @Test
    fun `Test InterTableModel#addRow() and InterTableModel#removeRow()`() {
        val row = Row.parse(1)
        intervalTable.intervalTableModel.addRow(row)

        assertEquals(data.size + 1, intervalTable.currentData.size)
        assertEquals(row, intervalTable.currentData.last())

        intervalTable.intervalTableModel.removeRow(0)

        assertEquals(data.size, intervalTable.currentData.size)
        assertFalse(intervalTable.currentData.contains(data.get(0)))
    }

    @ParameterizedTest
    @CsvSource(
        "0, 0",
        "0, 1",
        "1, 0",
        "1, 1",
        "2, 0",
        "2, 1"
    )
    fun `Test IntervalTableModel#isCellEditable to allows return true`(row: Int, column: Int) {
        assertTrue(intervalTable.intervalTableModel.isCellEditable(row, column))
    }
}
