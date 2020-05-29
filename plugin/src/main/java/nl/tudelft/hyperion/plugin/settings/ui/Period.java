package nl.tudelft.hyperion.plugin.settings.ui;

/**
 * Class that represents different periods that we allow to be used as intervals.
 */
public enum Period {
    SECONDS(1),
    MINUTES(60),
    HOURS(3600),
    DAYS(86400),
    WEEKS(604800);

    /**
     * Public field that specifies the amount of seconds a period is.
     * This is used to convert intervals to seconds {@link Row#toSeconds()}.
     */
    public final int inSeconds;

    /**
     * Instantiate Period with given amount of seconds.
     * @param inSeconds this periods value in seconds.
     */
    Period(int inSeconds) {
        this.inSeconds = inSeconds;
    }

    @Override
    public String toString() {
        return name().substring(0, 1) + name().substring(1).toLowerCase();
    }
}
