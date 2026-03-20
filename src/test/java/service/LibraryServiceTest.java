package service;

import model.Song;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LibraryServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void rootLevelSongUsesAlbumFallbackInsteadOfMusicFolderName() throws IOException {
        Path musicDir = Files.createDirectories(tempDir.resolve("music"));
        Files.writeString(musicDir.resolve("artist - title.mp3"), "demo");

        LibraryService service = new LibraryService(musicDir);
        List<Song> songs = service.loadLibrary();

        assertEquals(1, songs.size());
        assertEquals("Album", songs.getFirst().getDisplayAlbum());
    }

    @Test
    void missingYearUsesZeroPlaceholder() throws IOException {
        Path musicDir = Files.createDirectories(tempDir.resolve("music"));
        Files.writeString(musicDir.resolve("artist - title.mp3"), "demo");

        LibraryService service = new LibraryService(musicDir);
        List<Song> songs = service.loadLibrary();

        assertEquals(1, songs.size());
        assertEquals("00000", songs.getFirst().getDisplayYear());
    }
}
