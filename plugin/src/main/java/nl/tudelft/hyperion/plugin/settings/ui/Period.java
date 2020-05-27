package nl.tudelft.hyperion.plugin.settings.ui;

public enum Period {
    Seconds(1),
    Minutes(60),
    Hours(3600),
    Days(86400),
    Weeks(604800);

    public final int inSeconds;

    private Period(int inSeconds) {
        this.inSeconds = inSeconds;
    }
}
