package repository;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import mudelid.PlaylistSnapshot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON-backed repository for playlists.
 */
public final class JsonPlaylistRepository implements PlaylistRepository {
    private final Path filePath;
    private final ObjectMapper objectMapper;

    public JsonPlaylistRepository(Path filePath) {
        this.filePath = filePath;
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        initialiseFile();
    }

    @Override
    public synchronized List<PlaylistSnapshot> loadPlaylists() {
        return List.copyOf(readStore().playlists);
    }

    @Override
    public synchronized void savePlaylists(List<PlaylistSnapshot> playlists) {
        PlaylistStore store = readStore();
        store.playlists = new ArrayList<>(playlists);
        writeStore(store);
    }

    private void initialiseFile() {
        try {
            Files.createDirectories(filePath.getParent());
            if (Files.notExists(filePath)) {
                writeStore(new PlaylistStore());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialise playlist file: " + filePath, exception);
        }
    }

    private PlaylistStore readStore() {
        try {
            if (Files.notExists(filePath) || Files.size(filePath) == 0L) {
                return new PlaylistStore();
            }
            PlaylistStore store = objectMapper.readValue(filePath.toFile(), PlaylistStore.class);
            if (store.playlists == null) {
                store.playlists = new ArrayList<>();
            }
            return store;
        } catch (IOException exception) {
            return new PlaylistStore();
        }
    }

    private void writeStore(PlaylistStore store) {
        try {
            objectMapper.writeValue(filePath.toFile(), store);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write playlist file: " + filePath, exception);
        }
    }

    private static final class PlaylistStore {
        private List<PlaylistSnapshot> playlists = new ArrayList<>();
    }
}
