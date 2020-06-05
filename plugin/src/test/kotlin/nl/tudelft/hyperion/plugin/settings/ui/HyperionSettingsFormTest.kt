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
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HyperionSettingsFormTest {
    private val mockProject: Project = mockk()
    private lateinit var hyperionSettings: HyperionSettings
    private lateinit var hyperionSettingsForm: HyperionSettingsForm
    private val createTableMethod = getMethod("createTable")

    init {
        every { mockProject.name } returns "TestProject"
    }

    @BeforeEach
    fun setup() {
        hyperionSettings = HyperionSettings(mockProject)
        every { mockProject.getService(HyperionSettings::class.java) } returns hyperionSettings


        hyperionSettingsForm = HyperionSettingsForm(mockProject, true)

        getMethod("createSettings").call(hyperionSettingsForm)

        setProperty("addressField", JTextField(hyperionSettings.state.address))
        setProperty("projectField", JTextField(hyperionSettings.state.project))

        createTableMethod.call(hyperionSettingsForm)
    }

    @Test
    fun `Test createTable() and getIntervalRows() method`() {

        val expectedData = listOf(1, 3600, 3600*24, 3600*24*7)
        hyperionSettings.loadState(HyperionSettings.State().apply { intervals = expectedData })

        // We need the table to be constructed so we call the createTable method.
        // createTable uses the getIntervalRows method to fill the table with data.
        createTableMethod.call(hyperionSettingsForm)

        val intervalTable: IntervalTable = getPropertyValue("intervalTable")

        // Check if the data is the same
        assertEquals(expectedData, intervalTable.currentData.map(Row::toSeconds))
    }

    @Test
    fun `Test createTable with empty data`() {
        hyperionSettings.loadState(HyperionSettings.State().apply { intervals = emptyList() })

        // We need to construct the table again so it is empty this time.
        createTableMethod.call(hyperionSettingsForm)

        val intervalTable: IntervalTable = getPropertyValue("intervalTable")

        assertTrue(intervalTable.currentData.isEmpty())
    }

    @Test
    fun `Test createTable with default data`() {
        val intervalTable: IntervalTable = getPropertyValue("intervalTable")

        assertEquals(listOf(3600, 86400, 2592000), intervalTable.currentData.map(Row::toSeconds))
    }

    @Test
    fun `Test isModified due to intervalTable`() {
        val intervalTable: IntervalTable = getPropertyValue("intervalTable")

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
        val addressField: JTextField = getPropertyValue("addressField")
        val addressBefore = addressField.text

        assertFalse(hyperionSettingsForm.isModified)
        addressField.text = "test.address.com"
        assertTrue(hyperionSettingsForm.isModified)
        addressField.text = addressBefore
        assertFalse(hyperionSettingsForm.isModified)
    }

    @Test
    fun `Test isModified due to projectField`() {
        val projectField: JTextField = getPropertyValue("projectField")
        val projectBefore = projectField.text

        assertFalse(hyperionSettingsForm.isModified)
        projectField.text = "NewTestProject"
        assertTrue(hyperionSettingsForm.isModified)
        projectField.text = projectBefore
        assertFalse(hyperionSettingsForm.isModified)
    }

    @Test
    fun `Test apply with addressField`() {
        val addressField: JTextField = getPropertyValue("addressField")
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
        val projectField: JTextField = getPropertyValue("projectField")
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
        val intervalTable: IntervalTable = getPropertyValue("intervalTable")
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
        val projectField: JTextField = getPropertyValue("projectField")
        projectField.text = "NonExistingProject"
        val addressField: JTextField = getPropertyValue("addressField")
        addressField.text = "does.not.exist.com"
        val intervalTable: IntervalTable = getPropertyValue("intervalTable")
        intervalTable.intervalTableModel.removeRow(intervalTable.currentData.lastIndex)
        intervalTable.intervalTableModel.addRow(Row(999, Period.HOURS))

        assertTrue(hyperionSettingsForm.isModified)

        hyperionSettingsForm.reset()

        assertFalse(hyperionSettingsForm.isModified)
    }

    private fun getMethod(name: String): KFunction<*> {
        return HyperionSettingsForm::class.memberFunctions.find{it.name == name}
                ?.apply { isAccessible = true }
                ?: throw AssertionError("Could not find HyperionSettingsForm#$name method")
    }

    private fun getProperty(name: String): KProperty1<out HyperionSettingsForm, *> {
        return hyperionSettingsForm::class.memberProperties.find { it.name == name }
                ?.apply { isAccessible = true }
                ?: throw AssertionError("Could not find HyperionSettingsForm.$name property")
    }

    private fun <T> getPropertyValue(name: String): T {
        return getProperty(name).getter.call(hyperionSettingsForm) as T
    }

    private fun setProperty(name: String, value: Any) {
        (getProperty(name)
                as KMutableProperty1<out HyperionSettingsForm, *>)
                .setter.call(hyperionSettingsForm, value)
    }
}
