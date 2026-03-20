package service;

import model.PlayEvent;
import model.Song;
import model.SongStats;
import org.junit.jupiter.api.Test;
import repository.AnalyticsRepository;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticsServiceTest {
    @Test
    void qualifiedPlayIsStoredOnlyAfterThirtySeconds() {
        InMemoryAnalyticsRepository repository = new InMemoryAnalyticsRepository();
        AnalyticsService service = new AnalyticsService(repository, ZoneId.of("UTC"));
        Song song = new Song("song-1", "title", "artist", "album", "2024", Path.of("demo.mp3"));

        assertFalse(service.recordPlayIfQualified(song, 10));
        assertTrue(service.recordPlayIfQualified(song, 30));
        assertEquals(1, repository.loadPlays().size());
    }

    @Test
    void topSongsAggregatePlayCountAndDuration() {
        InMemoryAnalyticsRepository repository = new InMemoryAnalyticsRepository();
        repository.recordPlay(new PlayEvent("song-1", Instant.parse("2026-03-01T10:00:00Z"), 45));
        repository.recordPlay(new PlayEvent("song-1", Instant.parse("2026-03-02T10:00:00Z"), 60));
        repository.recordPlay(new PlayEvent("song-2", Instant.parse("2026-03-03T10:00:00Z"), 30));

        AnalyticsService service = new AnalyticsService(repository, ZoneId.of("UTC"));
        List<SongStats> topSongs = service.getTopSongs(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), 10);

        assertEquals("song-1", topSongs.getFirst().songId());
        assertEquals(2, topSongs.getFirst().playCount());
        assertEquals(105, topSongs.getFirst().totalPlayTimeSeconds());
    }

    @Test
    void playlistPlayIsStoredOnlyAfterEnoughQualifiedSongs() {
        InMemoryAnalyticsRepository repository = new InMemoryAnalyticsRepository();
        AnalyticsService service = new AnalyticsService(repository, ZoneId.of("UTC"));

        assertFalse(service.recordPlaylistPlayIfQualified("Focus", 1));
        assertTrue(service.recordPlaylistPlayIfQualified("Focus", 2));
        assertEquals(1, service.getPlaylistPlayCount("Focus"));
    }

    @Test
    void playlistPlayCountDefaultsToZero() {
        InMemoryAnalyticsRepository repository = new InMemoryAnalyticsRepository();
        AnalyticsService service = new AnalyticsService(repository, ZoneId.of("UTC"));

        assertEquals(0, service.getPlaylistPlayCount("Unknown"));
    }

    private static final class InMemoryAnalyticsRepository implements AnalyticsRepository {
        private final List<PlayEvent> plays = new ArrayList<>();
        private final Map<String, Integer> playlistPlayCounts = new LinkedHashMap<>();

        @Override
        public void recordPlay(PlayEvent event) {
            plays.add(event);
        }

        @Override
        public List<PlayEvent> loadPlays() {
            return List.copyOf(plays);
        }

        @Override
        public void recordPlaylistPlay(String playlistName) {
            playlistPlayCounts.put(playlistName, playlistPlayCounts.getOrDefault(playlistName, 0) + 1);
        }

        @Override
        public Map<String, Integer> loadPlaylistPlayCounts() {
            return Map.copyOf(playlistPlayCounts);
        }
    }
}
