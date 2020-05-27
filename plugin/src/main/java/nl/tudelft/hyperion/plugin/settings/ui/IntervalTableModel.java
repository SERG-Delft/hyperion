package nl.tudelft.hyperion.plugin.settings.ui;

import javax.swing.table.AbstractTableModel;

class IntervalTableModel extends AbstractTableModel {
    private Row[] data = {
            new Row(1, "Hours"),
            new Row(1, "Days"),
            new Row(1, "Months")
    };
    private final String[] columnNames = new String[]{"Interval", "Frequency"};

    public Row[] getData() {
        return data;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return data[0].getColumn(columnIndex).getClass();
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    /**
     * Returns the number of rows in the model. A
     * `JTable` uses this method to determine how many rows it
     * should display.
     *
     * @return the number of rows in the model
     * @see .getColumnCount
     */
    @Override
    public int getRowCount() {
        return data.length;
    }

    /**
     * Returns the number of columns in the model. A
     * `JTable` uses this method to determine how many columns it
     * should create and display by default.
     *
     * @return the number of columns in the model
     * @see .getRowCount
     */
    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    /**
     * Returns the value for the cell at <code>columnIndex</code> and
     * <code>rowIndex</code>.
     *
     * @param rowIndex    the row whose value is to be queried
     * @param columnIndex the column whose value is to be queried
     * @return the value Object at the specified cell
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return data[rowIndex].getColumn(columnIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        data[rowIndex].setColumn(columnIndex, aValue);
        fireTableCellUpdated(rowIndex, columnIndex);
    }
}
