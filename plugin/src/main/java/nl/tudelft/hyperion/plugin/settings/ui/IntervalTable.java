package nl.tudelft.hyperion.plugin.settings.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class that represents a table of configurable intervals.
 * Each Row has an integer field and a period field. The integer specifies the interval size and the
 * period scales it accordingly. For example a row can be {2, Hours} representing 2 Hours or 7200 seconds.
 */
public class IntervalTable extends JBTable {
    /**
     * List of Rows representing the data we saved last time
     * {@link HyperionSettingsForm#apply()} was pressed.
     */
    private List<Row> lastData;
    private final IntervalTableModel tableModel;

    /**
     * Instantiate new IntervalTable with given data to display.
     * @param data rows to display in the table.
     */
    public IntervalTable(List<Row> data) {
        super(new IntervalTableModel(cloneData(data)));
        TableColumn frequencyColumn = this.getColumnModel().getColumn(1);

        // We overwrite the CellEditor to display a dropdown menu containing all possible Periods {@link Period}
        JComboBox<Period> comboBox = new ComboBox<>(Period.class.getEnumConstants());
        frequencyColumn.setCellEditor(new DefaultCellEditor(comboBox));

        tableModel = ((IntervalTableModel) this.getModel());
        lastData = cloneData(getCurrentData());
    }

    /**
     * Gets the latest data from the table. This includes any changes the user has made
     * @return latest (potentially edited) data from table.
     */
    List<Row> getCurrentData() {
        return tableModel.getData();
    }

    /**
     * Resets the table's data to last saved data.
     */
    void reset() {
        tableModel.setData(cloneData(lastData));
    }

    /**
     * Returns the TableModel {@link IntervalTableModel} for this IntervalTable.
     * The Model contains data and information regarding column types etc.
     * @return the IntervalTableModel for this IntervalTable.
     */
    IntervalTableModel getIntervalTableModel() {
        return tableModel;
    }

    /**
     * This is called when the data currently in the table has been saved.
     * We thus update lastData and return the data we updated to.
     * @return the updated data.
     */
    List<Row> updateData() {
        return lastData = cloneData(getCurrentData());
    }

    /**
     * Checks if the table has been modified compared to the last time we saved.
     * @return boolean value that returns true when the current data has deviated from the last saved data.
     */
    boolean isModified() {
        return !lastData.equals(getCurrentData());
    }

    /**
     * Private method used to deep clone rows of data.
     * This is so we can compare the current data to the last saved data.
     * @param data data to clone.
     * @return Cloned data.
     */
    private static List<Row> cloneData(List<Row> data) {
        return data.stream().map(Row::clone).collect(Collectors.toList());
    }
}
