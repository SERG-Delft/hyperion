package nl.tudelft.hyperion.plugin.settings.ui;

import java.util.Objects;

class Row implements Cloneable {
    private int interval;
    private Period period;

    Row(int interval, Period period) {
        this.interval = interval;
        this.period = period;
    }

    static Row parse(int inSeconds) {
        if (inSeconds % Period.Weeks.inSeconds == 0)
            return new Row(inSeconds / Period.Weeks.inSeconds, Period.Weeks);
        if (inSeconds % Period.Days.inSeconds == 0)
            return new Row(inSeconds / Period.Days.inSeconds, Period.Days);
        if (inSeconds % Period.Hours.inSeconds == 0)
            return new Row(inSeconds / Period.Hours.inSeconds, Period.Hours);
        if (inSeconds % Period.Minutes.inSeconds == 0)
            return new Row(inSeconds / Period.Minutes.inSeconds, Period.Minutes);

        return new Row(inSeconds, Period.Seconds);
    }

    Object getColumn(int columnIndex) {
        if (columnIndex == 0) return interval;
        return period;
    }

    void setColumn(int columnIndex, Object value) {
        if (columnIndex == 0 && value instanceof Integer) interval = ((Integer) value);
        else if (value instanceof Period) period = ((Period) value);

    }

    int toSeconds() {
        return interval * period.inSeconds;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Row row = (Row) o;
        return interval == row.interval && period.equals(row.period);
    }

    @Override
    public int hashCode() {
        return Objects.hash(interval, period);
    }

    @Override
    public String toString() {
        return interval + " " + period;
    }

    public Row clone() {
        return new Row(interval, period);
    }
}
