package service;

import mudelid.Playlist;
import mudelid.PlaylistSnapshot;
import mudelid.Song;
import org.junit.jupiter.api.Test;
import repository.PlaylistRepository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaylistPersistenceServiceTest {
    @Test
    void loadPlaylistsMapsSongIdsToExistingSongs() {
        InMemoryPlaylistRepository repository = new InMemoryPlaylistRepository();
        repository.playlists = List.of(new PlaylistSnapshot("Focus", List.of("song-1", "song-missing")));

        PlaylistPersistenceService service = new PlaylistPersistenceService(repository);

        Song song = new Song("song-1", "Title", "Artist", "Album", Path.of("one.mp3"));
        Map<String, Song> songsById = new HashMap<>();
        songsById.put(song.getId(), song);

        List<Playlist> loaded = service.loadPlaylists(songsById);

        assertEquals(1, loaded.size());
        assertEquals("Focus", loaded.getFirst().getName());
        assertEquals(1, loaded.getFirst().getSongs().size());
        assertEquals("song-1", loaded.getFirst().getSongs().getFirst().getId());
    }

    @Test
    void savePlaylistsWritesSnapshots() {
        InMemoryPlaylistRepository repository = new InMemoryPlaylistRepository();
        PlaylistPersistenceService service = new PlaylistPersistenceService(repository);

        Song song = new Song("song-1", "Title", "Artist", "Album", Path.of("one.mp3"));
        Playlist playlist = new Playlist("Focus");
        playlist.addSong(song);

        service.savePlaylists(List.of(playlist));

        assertEquals(1, repository.playlists.size());
        assertEquals("Focus", repository.playlists.getFirst().name());
        assertEquals(List.of("song-1"), repository.playlists.getFirst().songIds());
    }

    private static final class InMemoryPlaylistRepository implements PlaylistRepository {
        private List<PlaylistSnapshot> playlists = new ArrayList<>();

        @Override
        public List<PlaylistSnapshot> loadPlaylists() {
            return List.copyOf(playlists);
        }

        @Override
        public void savePlaylists(List<PlaylistSnapshot> playlists) {
            this.playlists = new ArrayList<>(playlists);
        }
    }
}
