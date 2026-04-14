package service;

import mudelid.PlayEvent;
import mudelid.Song;
import mudelid.SongStats;
import repository.AnalyticsRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Keeps analytics logic out of the UI. The service only writes an event after a
 * song has been heard for at least 30 seconds.
 */
public final class AnalyticsService {
    public static final long QUALIFIED_PLAY_SECONDS = 30L;
    public static final int QUALIFIED_SONGS_FOR_PLAYLIST_PLAY = 2;

    private final AnalyticsRepository repository;
    private final ZoneId zoneId;

    public AnalyticsService(AnalyticsRepository repository) {
        this(repository, ZoneId.systemDefault());
    }

    public AnalyticsService(AnalyticsRepository repository, ZoneId zoneId) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId cannot be null");
    }

    public boolean recordPlayIfQualified(Song song, long secondsPlayed) {
        if (song == null || secondsPlayed < QUALIFIED_PLAY_SECONDS) {
            return false;
        }
        repository.recordPlay(new PlayEvent(song.getId(), Instant.now(), secondsPlayed));
        return true;
    }

    public SongStats getStats(String songId) {
        List<PlayEvent> matches = repository.loadPlays().stream()
                .filter(event -> event.songId().equals(songId))
                .toList();
        if (matches.isEmpty()) {
            return new SongStats(songId, 0, null, 0);
        }
        long totalSeconds = matches.stream().mapToLong(PlayEvent::secondsPlayed).sum();
        Instant lastPlayed = matches.stream().map(PlayEvent::timestamp).max(Comparator.naturalOrder()).orElse(null);
        return new SongStats(songId, matches.size(), lastPlayed, totalSeconds);
    }

    public boolean recordPlaylistPlayIfQualified(String playlistName, int qualifiedSongCount) {
        if (playlistName == null || playlistName.isBlank() || qualifiedSongCount < QUALIFIED_SONGS_FOR_PLAYLIST_PLAY) {
            return false;
        }
        repository.recordPlaylistPlay(playlistName.trim());
        return true;
    }

    public int getPlaylistPlayCount(String playlistName) {
        if (playlistName == null || playlistName.isBlank()) {
            return 0;
        }
        return repository.loadPlaylistPlayCounts().getOrDefault(playlistName.trim(), 0);
    }

    public List<SongStats> getTopSongs(LocalDate from, LocalDate to, int limit) {
        Instant fromInstant = from.atStartOfDay(zoneId).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(zoneId).toInstant();

        Map<String, List<PlayEvent>> grouped = repository.loadPlays().stream()
                .filter(event -> !event.timestamp().isBefore(fromInstant) && event.timestamp().isBefore(toInstant))
                .collect(Collectors.groupingBy(PlayEvent::songId));

        return grouped.entrySet().stream()
                .map(entry -> {
                    List<PlayEvent> events = entry.getValue();
                    long totalSeconds = events.stream().mapToLong(PlayEvent::secondsPlayed).sum();
                    Instant lastPlayed = events.stream().map(PlayEvent::timestamp).max(Comparator.naturalOrder()).orElse(null);
                    return new SongStats(entry.getKey(), events.size(), lastPlayed, totalSeconds);
                })
            .sorted(Comparator.comparingInt(SongStats::playCount).reversed()
                .thenComparing(Comparator.comparingLong(SongStats::totalPlayTimeSeconds).reversed()))
                .limit(limit)
                .toList();
    }

    public Map<String, SongStats> getStatsMap() {
        Map<String, SongStats> result = new LinkedHashMap<>();
        repository.loadPlays().stream()
                .map(PlayEvent::songId)
                .distinct()
                .forEach(songId -> result.put(songId, getStats(songId)));
        return result;
    }
}
