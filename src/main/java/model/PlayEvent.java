package model;

import java.time.Instant;
import java.util.Objects;

/**
 * Raw analytics event written to JSON. Keeping raw events makes it easy to
 * replace the repository with SQLite later without changing the service API.
 */
public record PlayEvent(String songId, Instant timestamp, long secondsPlayed) {
    public PlayEvent {
        Objects.requireNonNull(songId, "songId cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
    }
}
