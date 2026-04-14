package repository;

import mudelid.PlaylistSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonPlaylistRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsPlaylistsFromJson() {
        Path file = tempDir.resolve("playlists.json");
        JsonPlaylistRepository repository = new JsonPlaylistRepository(file);

        List<PlaylistSnapshot> snapshots = List.of(
                new PlaylistSnapshot("Focus", List.of("song-1", "song-2")),
                new PlaylistSnapshot("Drive", List.of("song-3"))
        );

        repository.savePlaylists(snapshots);

        JsonPlaylistRepository reloaded = new JsonPlaylistRepository(file);
        assertEquals(snapshots, reloaded.loadPlaylists());
    }
}
