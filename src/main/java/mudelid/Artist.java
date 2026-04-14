package mudelid;

import java.util.List;
import java.util.Objects;

/**
 * Simple read-only artist projection created from the loaded library.
 */
public record Artist(String name, List<Song> songs) {
    public Artist {
        Objects.requireNonNull(name, "name cannot be null");
        songs = List.copyOf(Objects.requireNonNull(songs, "songs cannot be null"));
    }
}
