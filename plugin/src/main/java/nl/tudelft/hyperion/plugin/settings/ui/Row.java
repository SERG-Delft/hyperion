package nl.tudelft.hyperion.plugin.settings.ui;

import java.util.Objects;

class Row {
    private int interval;
    private String frequency;

    Row(int interval, String frequency) {
        this.interval = interval;
        this.frequency = frequency;
    }

    Object getColumn(int columnIndex) {
        if (columnIndex == 0) return interval;
        return frequency;
    }

    void setColumn(int columnIndex, Object value) {
        if (columnIndex == 0 && value instanceof Integer) interval = ((Integer) value);
        else if (value instanceof String) frequency = ((String) value);

    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Row row = (Row) o;
        return interval == row.interval && frequency.equals(row.frequency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interval, frequency);
    }

    @Override
    public String toString() {
        return "Row{" +
                "interval=" + interval +
                ", frequency='" + frequency + '\'' +
                '}';
    }
}
