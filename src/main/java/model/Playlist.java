package model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * In-memory playlist model for the MVP.
 */
public final class Playlist {
    private String name;
    private final List<Song> songs = new ArrayList<>();

    public Playlist(String name) {
        rename(name);
    }

    public String getName() {
        return name;
    }

    public void rename(String newName) {
        String cleaned = Objects.requireNonNull(newName, "newName cannot be null").trim();
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Playlist name cannot be blank.");
        }
        this.name = cleaned;
    }

    public List<Song> getSongs() {
        return List.copyOf(songs);
    }

    public void addSong(Song song) {
        if (song != null && !songs.contains(song)) {
            songs.add(song);
        }
    }

    public void removeSong(Song song) {
        songs.remove(song);
    }

    @Override
    public String toString() {
        return name;
    }
}
