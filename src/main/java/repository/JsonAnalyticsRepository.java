package repository;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mudelid.PlayEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON-backed repository used in the MVP.
 */
public final class JsonAnalyticsRepository implements AnalyticsRepository {
    private final Path filePath;
    private final ObjectMapper objectMapper;

    public JsonAnalyticsRepository(Path filePath) {
        this.filePath = filePath;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        initialiseFile();
    }

    @Override
    public synchronized void recordPlay(PlayEvent event) {
        AnalyticsStore store = readStore();
        store.plays.add(event);
        writeStore(store);
    }

    @Override
    public synchronized List<PlayEvent> loadPlays() {
        return List.copyOf(readStore().plays);
    }

    @Override
    public synchronized void recordPlaylistPlay(String playlistName) {
        AnalyticsStore store = readStore();
        int current = store.playlistPlayCounts.getOrDefault(playlistName, 0);
        store.playlistPlayCounts.put(playlistName, current + 1);
        writeStore(store);
    }

    @Override
    public synchronized Map<String, Integer> loadPlaylistPlayCounts() {
        return Map.copyOf(readStore().playlistPlayCounts);
    }

    private void initialiseFile() {
        try {
            Files.createDirectories(filePath.getParent());
            if (Files.notExists(filePath)) {
                writeStore(new AnalyticsStore());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not initialise analytics file: " + filePath, exception);
        }
    }

    private AnalyticsStore readStore() {
        try {
            if (Files.notExists(filePath) || Files.size(filePath) == 0L) {
                return new AnalyticsStore();
            }
            AnalyticsStore store = objectMapper.readValue(filePath.toFile(), AnalyticsStore.class);
            if (store.plays == null) {
                store.plays = new ArrayList<>();
            }
            if (store.playlistPlayCounts == null) {
                store.playlistPlayCounts = new LinkedHashMap<>();
            }
            return store;
        } catch (IOException exception) {
            return new AnalyticsStore();
        }
    }

    private void writeStore(AnalyticsStore store) {
        try {
            objectMapper.writeValue(filePath.toFile(), store);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write analytics file: " + filePath, exception);
        }
    }

    private static final class AnalyticsStore {
        private List<PlayEvent> plays = new ArrayList<>();
        private Map<String, Integer> playlistPlayCounts = new LinkedHashMap<>();
    }
}
