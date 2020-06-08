package nl.tudelft.hyperion.plugin.settings.ui

import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import nl.tudelft.hyperion.plugin.settings.HyperionSettings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import javax.swing.JTextField

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HyperionSettingsFormTest {
    private val mockProject: Project = mockk()
    private lateinit var hyperionSettings: HyperionSettings
    private lateinit var hyperionSettingsForm: HyperionSettingsForm

    init {
        every { mockProject.name } returns "TestProject"
    }

    @BeforeEach
    fun setup() {
        hyperionSettings = HyperionSettings(mockProject)
        every { mockProject.getService(HyperionSettings::class.java) } returns hyperionSettings


        hyperionSettingsForm = HyperionSettingsForm(mockProject, true)

        hyperionSettingsForm.createSettings()

        hyperionSettingsForm.apply {
            addressField = JTextField(hyperionSettings.state.address)
            projectField = JTextField(hyperionSettings.state.project)
        }

        hyperionSettingsForm.createTable()
    }

    @Test
    fun `Test createTable() and getIntervalRows() method`() {

        val expectedData = listOf(1, 3600, 3600*24, 3600*24*7)
        hyperionSettings.loadState(HyperionSettings.State().apply { intervals = expectedData })

        // We need the table to be constructed so we call the createTable method.
        // createTable uses the getIntervalRows method to fill the table with data.
        hyperionSettingsForm.createTable()

        val intervalTable: IntervalTable = hyperionSettingsForm.intervalTable

        // Check if the data is the same
        assertEquals(expectedData, intervalTable.currentData.map(Row::toSeconds))
    }

    @Test
    fun `Test createTable with empty data`() {
        hyperionSettings.loadState(HyperionSettings.State().apply { intervals = emptyList() })

        // We need to construct the table again so it is empty this time.
        hyperionSettingsForm.createTable()

        val intervalTable: IntervalTable = hyperionSettingsForm.intervalTable

        assertTrue(intervalTable.currentData.isEmpty())
    }

    @Test
    fun `Test createTable with default data`() {
        val intervalTable: IntervalTable = hyperionSettingsForm.intervalTable

        assertEquals(listOf(3600, 86400, 2592000), intervalTable.currentData.map(Row::toSeconds))
    }

    @Test
    fun `Test isModified due to intervalTable`() {
        val intervalTable: IntervalTable = hyperionSettingsForm.intervalTable

        // Verify addition of new row.
        assertFalse(hyperionSettingsForm.isModified)
        intervalTable.intervalTableModel.addRow(Row(0, Period.DAYS))
        assertTrue(hyperionSettingsForm.isModified)
        // Verify rollback results in isModified() to return false again.
        intervalTable.intervalTableModel.removeRow(intervalTable.currentData.lastIndex)
        assertFalse(hyperionSettingsForm.isModified)

        // Verify removal of row.
        val row = intervalTable.currentData.last()
        intervalTable.intervalTableModel.removeRow(intervalTable.currentData.lastIndex)
        assertTrue(hyperionSettingsForm.isModified)
        // Verify rollback results in isModified() to return false again.
        intervalTable.intervalTableModel.addRow(row)
        assertFalse(hyperionSettingsForm.isModified)

        // Verify change in first column.
        val columnOne = intervalTable.currentData.first().getColumn(0)
        intervalTable.currentData.first().setColumn(0, -1)
        assertTrue(hyperionSettingsForm.isModified)
        // Verify rollback to return false.
        intervalTable.currentData.first().setColumn(0, columnOne)
        assertFalse(hyperionSettingsForm.isModified)

        // Verify change in second column.
        val columnTwo = intervalTable.currentData.first().getColumn(1)
        intervalTable.currentData.first().setColumn(1, Period.WEEKS)
        assertTrue(hyperionSettingsForm.isModified)
        // Verify rollback to return false.
        intervalTable.currentData.first().setColumn(1, columnTwo)
        assertFalse(hyperionSettingsForm.isModified)
    }

    @Test
    fun `Test isModified due to addressField`() {
        val addressField: JTextField = hyperionSettingsForm.addressField
        val addressBefore = addressField.text

        assertFalse(hyperionSettingsForm.isModified)
        addressField.text = "test.address.com"
        assertTrue(hyperionSettingsForm.isModified)
        addressField.text = addressBefore
        assertFalse(hyperionSettingsForm.isModified)
    }

    @Test
    fun `Test isModified due to projectField`() {
        val projectField: JTextField = hyperionSettingsForm.projectField
        val projectBefore = projectField.text

        assertFalse(hyperionSettingsForm.isModified)
        projectField.text = "NewTestProject"
        assertTrue(hyperionSettingsForm.isModified)
        projectField.text = projectBefore
        assertFalse(hyperionSettingsForm.isModified)
    }

    @Test
    fun `Test apply with addressField`() {
        val addressField: JTextField = hyperionSettingsForm.addressField
        val newAddress = "applied.address.com"
        addressField.text = newAddress

        // Normally the apply button can only be pressed if isModified returns true.
        assertTrue(hyperionSettingsForm.isModified)

        hyperionSettingsForm.apply()

        // The form should no longer be modified now that we applied.
        assertFalse(hyperionSettingsForm.isModified)

        // Check if the address was indeed applied.
        assertEquals(newAddress, hyperionSettings.state.address)
    }

    @Test
    fun `Test apply with projectField`() {
        val projectField: JTextField = hyperionSettingsForm.projectField
        val newProject = "AppliedProject"
        projectField.text = newProject

        // Normally the apply button can only be pressed if isModified returns true.
        assertTrue(hyperionSettingsForm.isModified)

        hyperionSettingsForm.apply()

        // The form should no longer be modified now that we applied.
        assertFalse(hyperionSettingsForm.isModified)

        // Check if the project was indeed applied.
        assertEquals(newProject, hyperionSettings.state.project)
    }

    @Test
    fun `Test apply with intervalTable`() {
        val intervalTable: IntervalTable = hyperionSettingsForm.intervalTable
        intervalTable.intervalTableModel.removeRow(intervalTable.currentData.lastIndex)
        intervalTable.intervalTableModel.addRow(Row(-1, Period.WEEKS))
        val newIntervals = intervalTable.currentData.map(Row::toSeconds)

        // Normally the apply button can only be pressed if isModified returns true.
        assertTrue(hyperionSettingsForm.isModified)

        hyperionSettingsForm.apply()

        // The form should no longer be modified now that we applied.
        assertFalse(hyperionSettingsForm.isModified)

        // Check if the intervals were indeed applied.
        assertEquals(newIntervals, hyperionSettings.state.intervals)
    }

    @Test
    fun `Test reset method`() {
        // Verify initial Form should not be modified.
        assertFalse(hyperionSettingsForm.isModified)

        // Change every modifiable object.
        val projectField: JTextField = hyperionSettingsForm.projectField
        projectField.text = "NonExistingProject"
        val addressField: JTextField = hyperionSettingsForm.addressField
        addressField.text = "does.not.exist.com"
        val intervalTable: IntervalTable = hyperionSettingsForm.intervalTable
        intervalTable.intervalTableModel.removeRow(intervalTable.currentData.lastIndex)
        intervalTable.intervalTableModel.addRow(Row(999, Period.HOURS))

        assertTrue(hyperionSettingsForm.isModified)

        hyperionSettingsForm.reset()

        assertFalse(hyperionSettingsForm.isModified)
    }
}
