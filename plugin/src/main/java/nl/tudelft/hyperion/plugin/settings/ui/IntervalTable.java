package nl.tudelft.hyperion.plugin.settings.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Objects;

public class IntervalTable extends JBTable {
    private Row[] lastData;
    private IntervalTableModel tableModel;
    public IntervalTable() {
        super(new IntervalTableModel());
        TableColumn frequencyColumn = this.getColumnModel().getColumn(1);
        JComboBox comboBox = new ComboBox<String>(new String[]{
                "Seconds",
                "Minutes",
                "Hours",
                "Days",
                "Weeks",
                "Months"
        });
        frequencyColumn.setCellEditor(new DefaultCellEditor(comboBox));

        tableModel = ((IntervalTableModel) this.getModel());
        lastData = Arrays.copyOf(getCurrentData(), getCurrentData().length);
    }

    Row[] getCurrentData() {

        return tableModel.getData();
    }

    Row[] updateData() {
        return lastData = getCurrentData().clone();
    }
    boolean isModified() {
        return !lastData.equals(getCurrentData());
    }
}