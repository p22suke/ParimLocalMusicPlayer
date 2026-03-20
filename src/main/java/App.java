import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import repository.JsonAnalyticsRepository;
import repository.JsonPlaylistRepository;
import service.AnalyticsService;
import service.LibraryService;
import service.PlaylistPersistenceService;
import service.PlaybackService;
import service.ThemeService;
import ui.MainView;

import java.nio.file.Path;

/**
 * Main entry point for ParimLocalMusicPlayer.
 */
public final class App extends Application {
    private MainView mainView;

    @Override
    public void start(Stage stage) {
        Path workspaceRoot = Path.of(System.getProperty("user.dir"));
        Path musicDirectory = workspaceRoot.resolve("data").resolve("music");
        Path analyticsFile = workspaceRoot.resolve("data").resolve("analytics").resolve("plays.json");
        Path playlistsFile = workspaceRoot.resolve("data").resolve("playlists").resolve("playlists.json");

        LibraryService libraryService = new LibraryService(musicDirectory);
        AnalyticsService analyticsService = new AnalyticsService(new JsonAnalyticsRepository(analyticsFile));
        PlaylistPersistenceService playlistPersistenceService = new PlaylistPersistenceService(new JsonPlaylistRepository(playlistsFile));
        PlaybackService playbackService = new PlaybackService(analyticsService);
        ThemeService themeService = new ThemeService();

        mainView = new MainView(libraryService, playbackService, analyticsService, themeService, playlistPersistenceService);

        Scene scene = new Scene(mainView, 420, 900);
        mainView.attachScene(scene);

        stage.setTitle("ParimLocalMusicPlayer");
        stage.setMinWidth(360);
        stage.setMinHeight(640);
        stage.setAlwaysOnTop(true);
        stage.setScene(scene);
        stage.show();
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
