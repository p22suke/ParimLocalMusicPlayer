package mudelid;

/**
 * Theme modes supported by the MVP.
 */
public enum ThemeMode {
    DAY,
    NIGHT,
    AUTO;

    public ThemeMode nextManualMode() {
        return switch (this) {
            case DAY -> NIGHT;
            case NIGHT -> AUTO;
            case AUTO -> DAY;
        };
    }
}
