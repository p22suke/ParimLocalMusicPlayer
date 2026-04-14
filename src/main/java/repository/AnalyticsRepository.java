//kaust to be renamed
package repository;
// java moodsa kirjanduse import (meie enda loodud klassid)
import mudelid.PlayEvent;
// java klassikalise kirjanduse import
import java.util.List;
import java.util.Map;

/**
 * AnalyticsRepository is an interface that defines methods for recording and retrieving analytics data related to play events 
 * and playlist plays. It allows for flexibility in the underlying data storage mechanism, enabling the MVP (Model-View-Presenter) 
 * architecture to start with a simple JSON-based implementation and later transition to a more robust solution like SQLite 
 * or a real database without affecting the rest of the application.
 */

public interface AnalyticsRepository {
    void recordPlay(PlayEvent event);

    List<PlayEvent> loadPlays();

    void recordPlaylistPlay(String playlistName);

    Map<String, Integer> loadPlaylistPlayCounts();
}
