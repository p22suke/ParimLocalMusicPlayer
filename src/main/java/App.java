import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.DirectoryChooser;
import repository.JsonAnalyticsRepository;
import repository.JsonPlaylistRepository;
import service.AnalyticsService;
import service.LibraryService;
import service.MetadataService;
import service.PlaylistPersistenceService;
import service.PlaybackService;
import service.ThemeService;
import meik.MainView;

import java.nio.file.Path;

/**
 * Main entry point for ParimLocalMusicPlayer.
 */
public final class App extends Application {
    private MainView mainView;

    @Override
    public void start(Stage stage) {
        Path workspaceRoot = Path.of(System.getProperty("user.dir"));
        Path defaultMusicDirectory = workspaceRoot.resolve("andmed").resolve("music");
        Path musicDirectory = chooseMusicDirectory(stage, defaultMusicDirectory);
        Path analyticsFile = workspaceRoot.resolve("andmed").resolve("analytics").resolve("plays.json");
        Path playlistsFile = workspaceRoot.resolve("andmed").resolve("playlists").resolve("playlists.json");

        LibraryService libraryService = new LibraryService(musicDirectory);
        AnalyticsService analyticsService = new AnalyticsService(new JsonAnalyticsRepository(analyticsFile));
        PlaylistPersistenceService playlistPersistenceService = new PlaylistPersistenceService(
                new JsonPlaylistRepository(playlistsFile));
        PlaybackService playbackService = new PlaybackService(analyticsService);
        ThemeService themeService = new ThemeService();
        MetadataService metadataService = new MetadataService();

        mainView = new MainView(libraryService, playbackService, analyticsService, themeService,
                playlistPersistenceService, metadataService);

        Scene scene = new Scene(mainView, 420, 900);
        mainView.attachScene(scene);

        stage.setTitle("ParimLocalMusicPlayer");
        stage.setMinWidth(360);
        stage.setMinHeight(640);
        stage.setAlwaysOnTop(true);
        stage.setScene(scene);
        stage.show();
    }

    private Path chooseMusicDirectory(Stage stage, Path fallbackDirectory) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select your music folder");

        Path candidateInitialDirectory = fallbackDirectory.toAbsolutePath().normalize();
        if (!candidateInitialDirectory.toFile().exists()) {
            candidateInitialDirectory = Path.of(System.getProperty("user.home"));
        }
        directoryChooser.setInitialDirectory(candidateInitialDirectory.toFile());

        java.io.File selectedDirectory = directoryChooser.showDialog(stage);
        if (selectedDirectory == null) {
            return fallbackDirectory;
        }
        return selectedDirectory.toPath().toAbsolutePath().normalize();
    }

    @Override
    public void stop() {
        if (mainView != null) {
            mainView.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
