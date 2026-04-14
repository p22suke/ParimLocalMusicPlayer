package service;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import mudelid.PlaybackQueue;
import mudelid.RepeatMode;
import mudelid.Song;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Wraps JavaFX MediaPlayer and exposes small observable properties the UI can
 * bind to.
 */
public final class PlaybackService {
    private final PlaybackQueue playbackQueue = new PlaybackQueue();
    private final AnalyticsService analyticsService;

    private final ObjectProperty<Song> currentSong = new SimpleObjectProperty<>();
    private final StringProperty currentContext = new SimpleStringProperty("Playing from: library");
    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);
    private final BooleanProperty playing = new SimpleBooleanProperty(false);
    private final ObjectProperty<RepeatMode> repeatMode = new SimpleObjectProperty<>(RepeatMode.OFF);
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final ReadOnlyListWrapper<Song> queueSnapshot = new ReadOnlyListWrapper<>(
            FXCollections.observableArrayList());

    private MediaPlayer mediaPlayer;
    private Song analyticsSong;
    private long playedSecondsForCurrentSong;
    private String analyticsContextName = "";
    private final Set<String> qualifiedSongIdsInContext = new HashSet<>();

    public PlaybackService(AnalyticsService analyticsService) {
        this.analyticsService = Objects.requireNonNull(analyticsService, "analyticsService cannot be null");
    }

    public void setQueue(List<Song> songs, String contextName, Song preferredSong, boolean autoPlay) {
        finalizePlaylistSessionIfContextChanged(contextName);
        playbackQueue.setQueue(songs, contextName, preferredSong);
        syncQueueState();
        if (autoPlay && playbackQueue.getCurrentSong() != null) {
            startSong(playbackQueue.getCurrentSong(), true);
        }
    }

    public void togglePlayPause() {
        if (mediaPlayer == null && playbackQueue.getCurrentSong() != null) {
            startSong(playbackQueue.getCurrentSong(), true);
            return;
        }
        if (mediaPlayer == null) {
            return;
        }
        MediaPlayer.Status status = mediaPlayer.getStatus();
        if (status == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            playing.set(false);
        } else {
            mediaPlayer.play();
            playing.set(true);
        }
    }

    public void stopNow() {
        stopPlayback();
    }

    public void next() {
        Song nextSong = playbackQueue.next();
        if (nextSong == null) {
            stopPlayback();
            return;
        }
        startSong(nextSong, true);
    }

    public void previous() {
        if (mediaPlayer != null && mediaPlayer.getCurrentTime().toSeconds() > 5) {
            mediaPlayer.seek(Duration.ZERO);
            return;
        }
        Song previousSong = playbackQueue.previous();
        if (previousSong != null) {
            startSong(previousSong, true);
        }
    }

    public void cycleRepeatMode() {
        repeatMode.set(playbackQueue.cycleRepeatMode());
    }

    public void seek(double progressFraction) {
        if (mediaPlayer == null || mediaPlayer.getTotalDuration().isUnknown()) {
            return;
        }
        Duration total = mediaPlayer.getTotalDuration();
        mediaPlayer.seek(total.multiply(Math.max(0.0, Math.min(progressFraction, 1.0))));
    }

    public void playSelectedSong(Song song, List<Song> queueSongs, String contextName) {
        playbackQueue.setQueue(queueSongs, contextName, song);
        syncQueueState();
        startSong(song, true);
    }

    public void addSongToPlayNext(Song song, List<Song> queueSongs, String contextName) {
        if (song == null) {
            return;
        }
        if (!playbackQueue.hasSongs()) {
            playbackQueue.setQueue(queueSongs, contextName, song);
        }
        playbackQueue.addAsNext(song);
        syncQueueState();
    }

    public ReadOnlyObjectProperty<Song> currentSongProperty() {
        return currentSong;
    }

    public ReadOnlyStringProperty currentContextProperty() {
        return currentContext;
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public BooleanProperty playingProperty() {
        return playing;
    }

    public ObjectProperty<RepeatMode> repeatModeProperty() {
        return repeatMode;
    }

    public ReadOnlyStringProperty errorMessageProperty() {
        return errorMessage;
    }

    public ReadOnlyListProperty<Song> queueSnapshotProperty() {
        return queueSnapshot.getReadOnlyProperty();
    }

    public void dispose() {
        stopAndDisposePlayer(true);
        finalizePlaylistSession();
    }

    private void startSong(Song song, boolean autoPlay) {
        if (song == null) {
            return;
        }
        stopAndDisposePlayer(true);
        errorMessage.set("");
        currentSong.set(song);
        currentContext.set("Playing from: " + playbackQueue.getContextName());
        analyticsContextName = playbackQueue.getContextName();
        analyticsSong = song;
        playedSecondsForCurrentSong = 0;

        try {
            Media media = new Media(song.getFilePath().toUri().toString());
            MediaPlayer newPlayer = new MediaPlayer(media);
            newPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
                Duration total = newPlayer.getTotalDuration();
                if (total != null && !total.isUnknown() && total.toMillis() > 0) {
                    progress.set(newValue.toMillis() / total.toMillis());
                }
                playedSecondsForCurrentSong = Math.max(playedSecondsForCurrentSong, (long) newValue.toSeconds());
            });
            newPlayer.setOnReady(() -> {
                progress.set(0.0);
                if (autoPlay) {
                    newPlayer.play();
                    playing.set(true);
                }
            });
            newPlayer.setOnPaused(() -> playing.set(false));
            newPlayer.setOnPlaying(() -> playing.set(true));
            newPlayer.setOnStopped(() -> playing.set(false));
            newPlayer.setOnEndOfMedia(this::handleEndOfMedia);
            newPlayer.setOnError(() -> {
                errorMessage.set("Playback error: " + song.getFilePath().getFileName());
                playing.set(false);
            });
            mediaPlayer = newPlayer;
            syncQueueState();
        } catch (Exception exception) {
            errorMessage.set("Playback error: " + song.getFilePath().getFileName());
            playing.set(false);
        }
    }

    private void handleEndOfMedia() {
        Song nextSong = playbackQueue.next();
        if (nextSong == null) {
            stopPlayback();
            return;
        }
        startSong(nextSong, true);
    }

    private void stopPlayback() {
        stopAndDisposePlayer(true);
        finalizePlaylistSession();
        progress.set(0.0);
        playing.set(false);
    }

    private void stopAndDisposePlayer(boolean persistAnalytics) {
        if (persistAnalytics && analyticsSong != null) {
            boolean qualified = analyticsService.recordPlayIfQualified(analyticsSong, playedSecondsForCurrentSong);
            if (qualified && isPlaylistContext(analyticsContextName)) {
                qualifiedSongIdsInContext.add(analyticsSong.getId());
            }
        }
        analyticsSong = null;
        playedSecondsForCurrentSong = 0;
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    private void syncQueueState() {
        queueSnapshot.setAll(playbackQueue.getActiveSongs());
        repeatMode.set(playbackQueue.getRepeatMode());
    }

    private void finalizePlaylistSessionIfContextChanged(String nextContextName) {
        if (!Objects.equals(analyticsContextName, nextContextName)) {
            finalizePlaylistSession();
        }
    }

    private void finalizePlaylistSession() {
        if (isPlaylistContext(analyticsContextName)) {
            analyticsService.recordPlaylistPlayIfQualified(
                    extractPlaylistName(analyticsContextName),
                    qualifiedSongIdsInContext.size());
        }
        qualifiedSongIdsInContext.clear();
        analyticsContextName = "";
    }

    private boolean isPlaylistContext(String contextName) {
        return contextName != null && contextName.startsWith("Playlist: ");
    }

    private String extractPlaylistName(String contextName) {
        if (!isPlaylistContext(contextName)) {
            return "";
        }
        return contextName.substring("Playlist: ".length()).trim();
    }
}
