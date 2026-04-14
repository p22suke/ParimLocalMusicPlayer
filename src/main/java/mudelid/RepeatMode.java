package mudelid;

/**
 * Repeat mode for the active playback queue.
 */
public enum RepeatMode {
    OFF,
    ONE,
    ALL;

    public RepeatMode next() {
        return switch (this) {
            case OFF -> ONE;
            case ONE -> ALL;
            case ALL -> OFF;
        };
    }
}
