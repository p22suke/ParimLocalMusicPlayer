package model;

import java.time.Instant;

/**
 * Aggregated analytics per song.
 */
public record SongStats(String songId, int playCount, Instant lastPlayed, long totalPlayTimeSeconds) {
}
