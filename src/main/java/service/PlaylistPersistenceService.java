package service;

import mudelid.Playlist;
import mudelid.PlaylistSnapshot;
import mudelid.Song;
import repository.PlaylistRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bridges UI playlist objects and persisted playlist snapshots.
 */
public final class PlaylistPersistenceService {
    private final PlaylistRepository repository;

    public PlaylistPersistenceService(PlaylistRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
    }

    public List<Playlist> loadPlaylists(Map<String, Song> songsById) {
        List<Playlist> playlists = new ArrayList<>();
        for (PlaylistSnapshot snapshot : repository.loadPlaylists()) {
            if (snapshot.name() == null || snapshot.name().isBlank()) {
                continue;
            }
            Playlist playlist;
            try {
                playlist = new Playlist(snapshot.name());
            } catch (IllegalArgumentException exception) {
                continue;
            }
            for (String songId : snapshot.songIds()) {
                Song song = songsById.get(songId);
                if (song != null) {
                    playlist.addSong(song);
                }
            }
            playlists.add(playlist);
        }
        playlists.sort(Comparator.comparing(Playlist::getName, String.CASE_INSENSITIVE_ORDER));
        return playlists;
    }

    public void savePlaylists(List<Playlist> playlists) {
        List<PlaylistSnapshot> snapshots = playlists.stream()
                .map(playlist -> new PlaylistSnapshot(
                        playlist.getName(),
                        playlist.getSongs().stream().map(Song::getId).toList()))
                .toList();
        repository.savePlaylists(snapshots);
    }
}
