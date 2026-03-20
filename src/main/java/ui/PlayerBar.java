package ui;

import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.MouseButton;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.RepeatMode;
import model.Song;
import service.PlaybackService;

import java.util.function.Consumer;

/**
 * Bottom floor player bar with compact now-playing metadata and transport
 * controls.
 */
public final class PlayerBar extends VBox {
    public PlayerBar(PlaybackService playbackService, Consumer<Song> likeSongHandler) {
        getStyleClass().addAll("section-pane", "player-bar");
        setSpacing(8);
        setPadding(new Insets(8, 10, 8, 10));

        Slider progressSlider = new Slider(0, 1, 0);
        progressSlider.setMaxWidth(Double.MAX_VALUE);
        playbackService.progressProperty().addListener((observable, oldValue, newValue) -> {
            if (!progressSlider.isValueChanging()) {
                progressSlider.setValue(newValue.doubleValue());
            }
        });
        progressSlider.valueChangingProperty().addListener((observable, wasChanging, isChanging) -> {
            if (!isChanging) {
                playbackService.seek(progressSlider.getValue());
            }
        });
        progressSlider.setOnMouseReleased(event -> playbackService.seek(progressSlider.getValue()));

        Button previousButton = new Button("<<");
        previousButton.setOnAction(event -> playbackService.previous());

        Button playPauseButton = new Button();
        playPauseButton.getStyleClass().add("primary-button");
        playPauseButton.textProperty().bind(Bindings.when(playbackService.playingProperty()).then("vaikus").otherwise("MUSS"));
        playPauseButton.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (event.getClickCount() >= 2) {
                playbackService.stopNow();
            } else {
                playbackService.togglePlayPause();
            }
        });

        Button nextButton = new Button(">>");
        nextButton.setOnAction(event -> playbackService.next());

        Button heartButton = new Button("♥");
        heartButton.getStyleClass().add("heart-button");
        heartButton.disableProperty().bind(playbackService.currentSongProperty().isNull());
        heartButton.setOnAction(event -> {
            Song song = playbackService.currentSongProperty().get();
            if (song != null) {
                likeSongHandler.accept(song);
            }
        });

        Button shuffleButton = new Button();
        shuffleButton.getStyleClass().add("status-toggle");
        shuffleButton.textProperty().bind(Bindings.when(playbackService.shuffleEnabledProperty()).then("Shuffle").otherwise("shuffle"));
        shuffleButton.setOnAction(event -> playbackService.toggleShuffle());
        updateToggleState(shuffleButton, playbackService.shuffleEnabledProperty().get());
        playbackService.shuffleEnabledProperty().addListener((observable, oldValue, newValue) ->
                updateToggleState(shuffleButton, newValue));

        Button repeatButton = new Button();
        repeatButton.getStyleClass().add("status-toggle");
        repeatButton.textProperty().bind(Bindings.createStringBinding(() -> {
            RepeatMode mode = playbackService.repeatModeProperty().get();
            return "repeat:" + mode.name().toLowerCase();
        }, playbackService.repeatModeProperty()));
        repeatButton.setOnAction(event -> playbackService.cycleRepeatMode());
        updateToggleState(repeatButton, playbackService.repeatModeProperty().get() != RepeatMode.OFF);
        playbackService.repeatModeProperty().addListener((observable, oldValue, newValue) ->
                updateToggleState(repeatButton, newValue != RepeatMode.OFF));

        HBox controls = new HBox(6, previousButton, playPauseButton, nextButton, heartButton, shuffleButton, repeatButton);
        controls.setAlignment(Pos.CENTER_LEFT);

        Label contextLabel = new Label();
        contextLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            Song song = playbackService.currentSongProperty().get();
            String context = playbackService.currentContextProperty().get();
            if (song == null) {
                return context == null || context.isBlank() ? "idle" : context;
            }
            String fileName = song.getFilePath().getFileName() == null
                    ? song.getFilePath().toString()
                    : song.getFilePath().getFileName().toString();
            String prefix = (context == null || context.isBlank()) ? "Playing from: unknown" : context;
            return prefix + " / " + fileName;
        }, playbackService.currentSongProperty(), playbackService.currentContextProperty()));

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.textProperty().bind(playbackService.errorMessageProperty());

        getChildren().addAll(progressSlider, controls, contextLabel, errorLabel);
        VBox.setVgrow(progressSlider, Priority.NEVER);
    }

    private static void updateToggleState(Button button, boolean isOn) {
        button.getStyleClass().removeAll("toggle-on", "toggle-off");
        button.getStyleClass().add(isOn ? "toggle-on" : "toggle-off");
    }
}
