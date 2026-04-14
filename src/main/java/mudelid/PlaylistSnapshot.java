package mudelid;

import java.util.List;
import java.util.Objects;

/**
 * Persisted representation of a playlist.
 */
public record PlaylistSnapshot(String name, List<String> songIds) {
    public PlaylistSnapshot {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(songIds, "songIds cannot be null");
        songIds = List.copyOf(songIds);
    }
}
