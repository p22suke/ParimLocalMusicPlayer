package repository;

import mudelid.PlaylistSnapshot;

import java.util.List;

/**
 * Playlist persistence abstraction.
 */
public interface PlaylistRepository {
    List<PlaylistSnapshot> loadPlaylists();

    void savePlaylists(List<PlaylistSnapshot> playlists);
}
