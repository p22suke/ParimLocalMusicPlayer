package model;

import java.util.List;
import java.util.Objects;

/**
 * Simple read-only album projection created from the loaded library.
 */
public record Album(String name, String artistName, List<Song> songs) {
    public Album {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(artistName, "artistName cannot be null");
        songs = List.copyOf(Objects.requireNonNull(songs, "songs cannot be null"));
    }
}
