package repository;

import model.PlayEvent;

import java.util.List;
import java.util.Map;

/**
 * Repository abstraction so the MVP can start with JSON and move to SQLite or a
 * real database later without changing the rest of the app.
 */
public interface AnalyticsRepository {
    void recordPlay(PlayEvent event);

    List<PlayEvent> loadPlays();

    void recordPlaylistPlay(String playlistName);

    Map<String, Integer> loadPlaylistPlayCounts();
}
