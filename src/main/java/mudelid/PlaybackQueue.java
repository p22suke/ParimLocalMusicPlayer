package mudelid;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Queue state is intentionally kept separate from the JavaFX media player so it
 * can be tested without touching UI or audio APIs.
 */
public final class PlaybackQueue {
    private final List<Song> sourceSongs = new ArrayList<>();
    private final List<Song> activeSongs = new ArrayList<>();

    private int currentIndex = -1;
    private RepeatMode repeatMode = RepeatMode.OFF;
    private String contextName = "All Songs";

    public void setQueue(List<Song> songs, String newContextName, Song preferredSong) {
        sourceSongs.clear();
        sourceSongs.addAll(Objects.requireNonNullElse(songs, List.of()));
        contextName = (newContextName == null || newContextName.isBlank()) ? "All Songs" : newContextName;
        rebuildActiveSongs(preferredSong);
    }

    public List<Song> getActiveSongs() {
        return List.copyOf(activeSongs);
    }

    public Song getCurrentSong() {
        if (currentIndex < 0 || currentIndex >= activeSongs.size()) {
            return null;
        }
        return activeSongs.get(currentIndex);
    }

    public String getContextName() {
        return contextName;
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = Objects.requireNonNull(repeatMode, "repeatMode cannot be null");
    }

    public RepeatMode cycleRepeatMode() {
        repeatMode = repeatMode.next();
        return repeatMode;
    }

    public boolean hasSongs() {
        return !activeSongs.isEmpty();
    }

    public Song moveToSong(Song song) {
        if (song == null) {
            return getCurrentSong();
        }
        int index = activeSongs.indexOf(song);
        if (index >= 0) {
            currentIndex = index;
        }
        return getCurrentSong();
    }

    public Song next() {
        if (activeSongs.isEmpty()) {
            return null;
        }
        if (repeatMode == RepeatMode.ONE) {
            return getCurrentSong();
        }
        if (currentIndex + 1 < activeSongs.size()) {
            currentIndex++;
            return getCurrentSong();
        }
        if (repeatMode == RepeatMode.ALL) {
            currentIndex = 0;
            return getCurrentSong();
        }
        return null;
    }

    public Song previous() {
        if (activeSongs.isEmpty()) {
            return null;
        }
        if (repeatMode == RepeatMode.ONE) {
            return getCurrentSong();
        }
        if (currentIndex - 1 >= 0) {
            currentIndex--;
            return getCurrentSong();
        }
        if (repeatMode == RepeatMode.ALL) {
            currentIndex = activeSongs.size() - 1;
            return getCurrentSong();
        }
        currentIndex = 0;
        return getCurrentSong();
    }

    public void addAsNext(Song song) {
        if (song == null) {
            return;
        }

        if (activeSongs.isEmpty()) {
            sourceSongs.clear();
            sourceSongs.add(song);
            activeSongs.clear();
            activeSongs.add(song);
            currentIndex = 0;
            return;
        }

        Song current = getCurrentSong();

        int existingActiveIndex = activeSongs.indexOf(song);
        if (existingActiveIndex >= 0) {
            activeSongs.remove(existingActiveIndex);
            if (existingActiveIndex < currentIndex) {
                currentIndex--;
            }
        }
        int nextIndex = Math.min(currentIndex + 1, activeSongs.size());
        activeSongs.add(nextIndex, song);

        sourceSongs.remove(song);
        int sourceCurrentIndex = sourceSongs.indexOf(current);
        int sourceInsertIndex = sourceCurrentIndex < 0 ? sourceSongs.size() : sourceCurrentIndex + 1;
        sourceSongs.add(Math.min(sourceInsertIndex, sourceSongs.size()), song);
    }

    private void rebuildActiveSongs(Song preferredSong) {
        activeSongs.clear();
        activeSongs.addAll(sourceSongs);
        if (activeSongs.isEmpty()) {
            currentIndex = -1;
            return;
        }

        Song songToKeep = preferredSong != null ? preferredSong : getSafeSongFromSource();
        currentIndex = Math.max(0, activeSongs.indexOf(songToKeep));
    }

    private Song getSafeSongFromSource() {
        if (currentIndex >= 0 && currentIndex < activeSongs.size()) {
            Song current = activeSongs.get(currentIndex);
            if (sourceSongs.contains(current)) {
                return current;
            }
        }
        return sourceSongs.getFirst();
    }
}
