package nl.tudelft.hyperion.plugin.settings.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.table.JBTable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IntervalTable extends JBTable {
    private List<Row> lastData;
    private final IntervalTableModel tableModel;
    public IntervalTable(List<Row> data) {
        super(new IntervalTableModel(data));
        TableColumn frequencyColumn = this.getColumnModel().getColumn(1);
        JComboBox comboBox = new ComboBox<>(Period.class.getEnumConstants());
        frequencyColumn.setCellEditor(new DefaultCellEditor(comboBox));

        tableModel = ((IntervalTableModel) this.getModel());
        lastData = cloneData(getCurrentData());
    }

    List<Row> getCurrentData() {
        return tableModel.getData();
    }

    void reset() {
        tableModel.setData(cloneData(lastData));
    }

    IntervalTableModel getIntervalTableModel() {
        return tableModel;
    }
    List<Row> updateData() {
        return lastData = cloneData(getCurrentData());
    }
    boolean isModified() {
        return !lastData.equals(getCurrentData());
    }

    private List<Row> cloneData(List<Row> data) {
        return data.stream().map(Row::clone).collect(Collectors.toList());
    }
}
