package nl.tudelft.hyperion.plugin.settings.ui;

import java.util.Objects;

/**
 * Class that represents a Row in the IntervalTable {@link IntervalTable}.
 */
class Row implements Cloneable {
    private int interval;
    private Period period;

    /**
     * Instantiate Row with given interval and period {@link Period}.
     * @param interval interval value (not necessarily in seconds).
     * @param period the period for the interval.
     */
    Row(int interval, Period period) {
        this.interval = interval;
        this.period = period;
    }

    /**
     * Creates a Row using the amount of specified seconds.
     * The Period for the Row is determined by the greatest possible fit without any seconds left.
     * Examples:
     * parse(3600) returns Row(1, Period.Hours)
     * parse(3660) returns Row(61, Period.Minutes)
     * parse(60) returns Row(1, Period.Minutes)
     * parse(61) returns Row(61, Period.Seconds)
     *
     * @param inSeconds interval in seconds
     * @return a Row with given interval, clipped to greatest possible Period.
     */
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

    /**
     * Returns the column value of the row.
     * If columnIndex is 0 we return the first column (interval), otherwise we return the second column (period)
     * @param columnIndex column to retrieve.
     * @return column relating to given index.
     */
    Object getColumn(int columnIndex) {
        if (columnIndex == 0) return interval;
        return period;
    }

    /**
     * Sets the specified column to specified value.
     * Will do nothing if value is not of correct type.
     * @param columnIndex column to change.
     * @param value value to apply.
     */
    void setColumn(int columnIndex, Object value) {
        if (columnIndex == 0 && value instanceof Integer) interval = ((Integer) value);
        else if (value instanceof Period) period = ((Period) value);

    }

    /**
     * Returns this Row's interval value in seconds
     * by multiplying it with the Period in seconds {@link Period#inSeconds}
     *
     * @return this Row's interval value in seconds.
     */
    int toSeconds() {
        return interval * period.inSeconds;
    }

    /**
     * Clones the values of this row to a new one.
     * @return a new Row with the same values.
     */
    public Row clone() {
        try {
            return ((Row) super.clone());
        } catch (CloneNotSupportedException e) {
            return new Row(interval, period);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Row row = (Row) o;
        return interval == row.interval && period.equals(row.period);
    }
}
