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
        lastData = getCurrentData().stream().map(row -> row.clone()).collect(Collectors.toList());
    }

    List<Row> getCurrentData() {
        return tableModel.getData();
    }

    void setData(List<Row> data) {
        lastData = data;
    }

    IntervalTableModel getIntervalTableModel() {
        return tableModel;
    }
    List<Row> updateData() {
        return lastData = new ArrayList<>(getCurrentData());
    }
    boolean isModified() {
        return !lastData.equals(getCurrentData());
    }

}
