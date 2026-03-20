package ui;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import model.Album;
import model.Artist;
import model.Playlist;
import model.Song;
import model.SongStats;
import model.ThemeMode;
import service.AnalyticsService;
import service.LibraryService;
import service.PlaylistPersistenceService;
import service.PlaybackService;
import service.ThemeService;

import java.awt.Desktop;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Main screen for the MVP.
 *
 * The app keeps one narrow stacked layout at all times:
 * - top floor: navigation
 * - middle floor: searchable content
 * - bottom floor: player bar
 */
public final class MainView extends VBox {
    private final LibraryService libraryService;
    private final PlaybackService playbackService;
    private final AnalyticsService analyticsService;
    private final ThemeService themeService;
    private final PlaylistPersistenceService playlistPersistenceService;

    private final ObservableList<Song> songItems = FXCollections.observableArrayList();
    private final ObservableList<Album> albumItems = FXCollections.observableArrayList();
    private final ObservableList<Artist> artistItems = FXCollections.observableArrayList();
    private final ObservableList<YearBucket> yearItems = FXCollections.observableArrayList();
    private final ObservableList<Playlist> playlistItems = FXCollections.observableArrayList();
    private final ObservableList<Song> playlistSongItems = FXCollections.observableArrayList();
    private final ObservableList<String> memoryItems = FXCollections.observableArrayList();

    private final List<Song> librarySongs = new ArrayList<>();
    private final Map<String, Song> songsById = new HashMap<>();

    private final TextField searchField = new TextField();
    private final TableView<Song> songTable = createSongTable();
    private final TableView<Song> playlistSongTable = createSongTable();
    private final ListView<Album> albumListView = new ListView<>(albumItems);
    private final ListView<Artist> artistListView = new ListView<>(artistItems);
    private final ListView<YearBucket> yearListView = new ListView<>(yearItems);
    private final ListView<Playlist> playlistListView = new ListView<>(playlistItems);
    private final ListView<String> memoryListView = new ListView<>(memoryItems);
    private final Hyperlink backLink = new Hyperlink("back");
    private final Label contentHeader = new Label();
    private final Label helperLabel = new Label();
    private final Label playlistHeader = new Label("playlist songs");
    private final Label playlistHelper = new Label();
    private final VBox onboardingPane = new VBox(6);
    private final Label onboardingTitle = new Label("onboarding");
    private final Label onboardingHelper = new Label();
    private final StringProperty themeLabel = new SimpleStringProperty("theme: auto");

    private final StackPane centerStack = new StackPane();
    private final VBox songPane = new VBox(8);
    private final VBox playlistsPane = new VBox(8);
    private final VBox memoryPane = new VBox(8);

    private final List<Album> allAlbums = new ArrayList<>();
    private final List<Artist> allArtists = new ArrayList<>();
    private final List<YearBucket> allYears = new ArrayList<>();
    private final List<Playlist> allPlaylists = new ArrayList<>();
    private final List<String> allMemoryRows = new ArrayList<>();

    private List<Song> activeSongSource = List.of();
    private String activeSongContext = "All Songs";
    private Playlist selectedPlaylist;
    private ThemeMode themeMode = ThemeMode.AUTO;
    private ViewState currentViewState = ViewState.ALL_SONGS;
    private Scene scene;
    private Timeline themeRefreshTimeline;

    public MainView(LibraryService libraryService,
                    PlaybackService playbackService,
                    AnalyticsService analyticsService,
                    ThemeService themeService,
                    PlaylistPersistenceService playlistPersistenceService) {
        this.libraryService = libraryService;
        this.playbackService = playbackService;
        this.analyticsService = analyticsService;
        this.themeService = themeService;
        this.playlistPersistenceService = playlistPersistenceService;

        getStyleClass().add("app-root");
        setSpacing(0);
        setFillWidth(true);

        NavigationBar navigationBar = new NavigationBar(this::handleNavigation, this::cycleThemeMode, themeLabel);
        PlayerBar playerBar = new PlayerBar(playbackService);
        VBox middleFloor = buildMiddleFloor();

        navigationBar.prefHeightProperty().bind(heightProperty().multiply(0.27));
        middleFloor.prefHeightProperty().bind(heightProperty().multiply(0.63));
        playerBar.prefHeightProperty().bind(heightProperty().multiply(0.10));
        VBox.setVgrow(middleFloor, Priority.ALWAYS);

        getChildren().addAll(navigationBar, middleFloor, playerBar);

        configureCollectionViews();
        installSongTableInteractions(songTable, () -> activeSongSource, () -> activeSongContext, false);
        installSongTableInteractions(playlistSongTable,
                () -> selectedPlaylist == null ? List.of() : selectedPlaylist.getSongs(),
                () -> selectedPlaylist == null ? "Playlist" : "Playlist: " + selectedPlaylist.getName(),
                true);

        playbackService.queueSnapshotProperty().addListener((ListChangeListener<? super Song>) change -> {
            if (currentViewState == ViewState.QUEUE) {
                activeSongSource = List.copyOf(playbackService.queueSnapshotProperty());
                applySearchFilter();
            }
        });
        playbackService.playingProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue && currentViewState == ViewState.PLAYLISTS) {
                playlistListView.refresh();
            }
        });

        loadLibrary();
        showAllSongs();
        updateThemeLabel();
        startThemeRefreshLoop();
    }

    public void attachScene(Scene scene) {
        this.scene = scene;
        applyTheme();
    }

    public void shutdown() {
        playbackService.dispose();
        if (themeRefreshTimeline != null) {
            themeRefreshTimeline.stop();
        }
    }

    private VBox buildMiddleFloor() {
        VBox middleFloor = new VBox(8);
        middleFloor.getStyleClass().addAll("section-pane", "middle-floor");
        middleFloor.setPadding(new Insets(10));

        searchField.setPromptText("search current view");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applySearchFilter());

        backLink.getStyleClass().add("back-link");
        backLink.setOnAction(event -> navigateBack());
        backLink.setManaged(false);
        backLink.setVisible(false);

        contentHeader.getStyleClass().add("section-header");
        helperLabel.getStyleClass().add("helper-label");
        helperLabel.setWrapText(true);
        helperLabel.setMaxWidth(Double.MAX_VALUE);

        HBox titleRow = new HBox(8, backLink, contentHeader);
        titleRow.setFillHeight(true);

        songPane.getChildren().addAll(titleRow, helperLabel, songTable);
        VBox.setVgrow(songTable, Priority.ALWAYS);

        buildOnboardingPane();
        buildPlaylistsPane();
        buildMemoryPane();

        centerStack.getChildren().addAll(songPane, albumListView, artistListView, yearListView, playlistsPane, memoryPane);
        showNode(songPane);
        VBox.setVgrow(centerStack, Priority.ALWAYS);

        middleFloor.getChildren().addAll(onboardingPane, searchField, centerStack);
        return middleFloor;
    }

    private void buildOnboardingPane() {
        onboardingPane.getStyleClass().add("onboarding-pane");
        onboardingTitle.getStyleClass().add("section-header");
        onboardingHelper.getStyleClass().add("helper-label");
        onboardingHelper.setWrapText(true);
        onboardingHelper.setMaxWidth(Double.MAX_VALUE);

        Label patternLabel = new Label("filename pattern: ARTIST - ALBUM - YEAR - TITLE.mp3");
        patternLabel.getStyleClass().add("helper-label");
        patternLabel.setWrapText(true);
        patternLabel.setMaxWidth(Double.MAX_VALUE);

        Hyperlink openFolderLink = new Hyperlink("open music folder");
        openFolderLink.setOnAction(event -> openMusicFolder());

        Hyperlink rescanLink = new Hyperlink("rescan library");
        rescanLink.setOnAction(event -> rescanLibrary());

        onboardingPane.getChildren().addAll(onboardingTitle, onboardingHelper, patternLabel, openFolderLink, rescanLink);
    }

    private void buildPlaylistsPane() {
        Hyperlink createLink = new Hyperlink("new playlist");
        createLink.setOnAction(event -> createPlaylist());

        Hyperlink renameLink = new Hyperlink("rename playlist");
        renameLink.setOnAction(event -> renamePlaylist());

        Hyperlink deleteLink = new Hyperlink("delete playlist");
        deleteLink.setOnAction(event -> deletePlaylist());

        Hyperlink playLink = new Hyperlink("play playlist");
        playLink.setOnAction(event -> playSelectedPlaylist());

        HBox controls = new HBox(10, createLink, renameLink, deleteLink, playLink);
        controls.setFillHeight(true);

        playlistHeader.getStyleClass().add("section-header");
        playlistHelper.getStyleClass().add("helper-label");
        playlistHelper.setWrapText(true);
        playlistHelper.setMaxWidth(Double.MAX_VALUE);

        playlistListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        playlistListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Playlist item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                int playCount = analyticsService.getPlaylistPlayCount(item.getName());
                setText(item.getName() + " (songs=" + item.getSongs().size() + ", plays=" + playCount + ")");
            }
        });
        playlistListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            selectedPlaylist = newValue;
            updateSelectedPlaylist();
        });
        playlistListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                playSelectedPlaylist();
            }
        });
        playlistListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                playSelectedPlaylist();
            }
        });

        VBox.setVgrow(playlistListView, Priority.ALWAYS);
        VBox.setVgrow(playlistSongTable, Priority.ALWAYS);
        playlistsPane.getChildren().addAll(controls, playlistHeader, playlistHelper, playlistListView, playlistSongTable);
    }

    private void buildMemoryPane() {
        Label memoryHeader = new Label("memory (hidden analytics)");
        memoryHeader.getStyleClass().add("section-header");
        Label memoryHelper = new Label("Monthly and yearly memory views will expand in v2.");
        memoryHelper.getStyleClass().add("helper-label");
        memoryHelper.setWrapText(true);
        memoryHelper.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(memoryListView, Priority.ALWAYS);
        memoryPane.getChildren().addAll(memoryHeader, memoryHelper, memoryListView);
    }

    private void configureCollectionViews() {
        albumListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Album item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name() + " -- " + item.artistName() + " (" + item.songs().size() + ")");
            }
        });
        artistListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Artist item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name() + " (" + item.songs().size() + ")");
            }
        });
        yearListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(YearBucket item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.year() + " (" + item.songs().size() + ")");
            }
        });

        albumListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Album selected = albumListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openAlbum(selected);
                }
            }
        });
        artistListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Artist selected = artistListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openArtist(selected);
                }
            }
        });
        yearListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                YearBucket selected = yearListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openYear(selected);
                }
            }
        });

        albumListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Album selected = albumListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openAlbum(selected);
                }
            }
        });
        artistListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Artist selected = artistListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openArtist(selected);
                }
            }
        });
        yearListView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                YearBucket selected = yearListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openYear(selected);
                }
            }
        });
    }

    private TableView<Song> createSongTable() {
        TableView<Song> table = new TableView<>();
        table.getStyleClass().add("song-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No songs in this view."));
        table.getColumns().add(TableCellRenderer.createSongColumn("song", Song::getDisplayTitle, 0.32));
        table.getColumns().add(TableCellRenderer.createSongColumn("artist", Song::getDisplayArtist, 0.24));
        table.getColumns().add(TableCellRenderer.createSongColumn("album", Song::getDisplayAlbum, 0.28));
        table.getColumns().add(TableCellRenderer.createSongColumn("year", Song::getDisplayYear, 0.16));
        return table;
    }

    private void installSongTableInteractions(TableView<Song> table,
                                              SongListSupplier queueSupplier,
                                              StringSupplier contextSupplier,
                                              boolean allowPlaylistRemoval) {
        table.setRowFactory(view -> {
            TableRow<Song> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    playSong(row.getItem(), queueSupplier.get(), contextSupplier.get());
                }
            });
            return row;
        });

        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Song selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    playSong(selected, queueSupplier.get(), contextSupplier.get());
                }
            }
        });

        table.setOnContextMenuRequested(event -> {
            Song selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                return;
            }
            javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();

            javafx.scene.control.MenuItem playNextItem = new javafx.scene.control.MenuItem("play next");
            playNextItem.setOnAction(actionEvent -> playbackService.addSongToPlayNext(selected, queueSupplier.get(), contextSupplier.get()));
            contextMenu.getItems().add(playNextItem);

            javafx.scene.control.Menu addToPlaylist = new javafx.scene.control.Menu("add to playlist");
            if (allPlaylists.isEmpty()) {
                javafx.scene.control.MenuItem emptyItem = new javafx.scene.control.MenuItem("create playlist first");
                emptyItem.setDisable(true);
                addToPlaylist.getItems().add(emptyItem);
            } else {
                for (Playlist playlist : allPlaylists) {
                    javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(playlist.getName());
                    item.setOnAction(actionEvent -> {
                        playlist.addSong(selected);
                        refreshPlaylistState();
                        persistPlaylists();
                    });
                    addToPlaylist.getItems().add(item);
                }
            }
            contextMenu.getItems().add(addToPlaylist);

            if (allowPlaylistRemoval && selectedPlaylist != null) {
                javafx.scene.control.MenuItem removeItem = new javafx.scene.control.MenuItem("remove from playlist");
                removeItem.setOnAction(actionEvent -> {
                    selectedPlaylist.removeSong(selected);
                    refreshPlaylistState();
                    persistPlaylists();
                });
                contextMenu.getItems().add(removeItem);
            }

            contextMenu.show(table, event.getScreenX(), event.getScreenY());
        });
    }

    private void playSong(Song song, List<Song> queueSongs, String contextName) {
        if (song == null || queueSongs.isEmpty()) {
            return;
        }
        playbackService.playSelectedSong(song, queueSongs, contextName);
    }

    private void handleNavigation(NavigationBar.Section section) {
        switch (section) {
            case ALL_SONGS -> showAllSongs();
            case ALBUMS -> showAlbums();
            case ARTISTS -> showArtists();
            case YEARS -> showYears();
            case PLAYLISTS -> showPlaylists();
            case QUEUE -> showQueue();
            case MEMORY -> showMemory();
        }
    }

    private void showAllSongs() {
        currentViewState = ViewState.ALL_SONGS;
        activeSongSource = List.copyOf(librarySongs);
        activeSongContext = "All Songs";
        contentHeader.setText("all songs");
        helperLabel.setText(librarySongs.isEmpty()
            ? "Put MP3 or WAV files in data/music, then use the rescan link in onboarding."
                : "Use Up/Down to move, Enter to play, right-click to add to playlist or play next.");
        playbackService.setQueue(activeSongSource, activeSongContext, activeSongSource.isEmpty() ? null : activeSongSource.getFirst(), false);
        showNode(songPane);
        updateBackButtonState();
        applySearchFilter();
    }

    private void showAlbums() {
        currentViewState = ViewState.ALBUMS;
        showNode(albumListView);
        updateBackButtonState();
        applySearchFilter();
    }

    private void showArtists() {
        currentViewState = ViewState.ARTISTS;
        showNode(artistListView);
        updateBackButtonState();
        applySearchFilter();
    }

    private void showYears() {
        currentViewState = ViewState.YEARS;
        showNode(yearListView);
        updateBackButtonState();
        applySearchFilter();
    }

    private void showPlaylists() {
        currentViewState = ViewState.PLAYLISTS;
        showNode(playlistsPane);
        updateBackButtonState();
        if (selectedPlaylist == null && !playlistItems.isEmpty()) {
            playlistListView.getSelectionModel().selectFirst();
        } else {
            updateSelectedPlaylist();
        }
        applySearchFilter();
    }

    private void showQueue() {
        currentViewState = ViewState.QUEUE;
        activeSongSource = List.copyOf(playbackService.queueSnapshotProperty());
        activeSongContext = "Queue";
        contentHeader.setText("queue");
        helperLabel.setText("This is the active playback queue. Shuffle and repeat only affect this list.");
        showNode(songPane);
        updateBackButtonState();
        applySearchFilter();
    }

    private void showMemory() {
        currentViewState = ViewState.MEMORY;
        showNode(memoryPane);
        updateBackButtonState();
        refreshMemoryView();
        applySearchFilter();
    }

    private void openAlbum(Album album) {
        currentViewState = ViewState.ALBUM_DETAIL;
        activeSongSource = List.copyOf(album.songs());
        activeSongContext = "Album: " + album.name();
        contentHeader.setText(album.name());
        helperLabel.setText("Album detail. Press Albums to go back to the album list.");
        playbackService.setQueue(activeSongSource, activeSongContext, activeSongSource.isEmpty() ? null : activeSongSource.getFirst(), false);
        showNode(songPane);
        updateBackButtonState();
        applySearchFilter();
    }

    private void openArtist(Artist artist) {
        currentViewState = ViewState.ARTIST_DETAIL;
        activeSongSource = List.copyOf(artist.songs());
        activeSongContext = "Artist: " + artist.name();
        contentHeader.setText(artist.name());
        helperLabel.setText("Artist detail. Press Artists to go back to the artist list.");
        playbackService.setQueue(activeSongSource, activeSongContext, activeSongSource.isEmpty() ? null : activeSongSource.getFirst(), false);
        showNode(songPane);
        updateBackButtonState();
        applySearchFilter();
    }

    private void openYear(YearBucket yearBucket) {
        currentViewState = ViewState.YEAR_DETAIL;
        activeSongSource = List.copyOf(yearBucket.songs());
        activeSongContext = "Year: " + yearBucket.year();
        contentHeader.setText("year " + yearBucket.year());
        helperLabel.setText("Year detail. Press Years to go back to the year list.");
        playbackService.setQueue(activeSongSource, activeSongContext, activeSongSource.isEmpty() ? null : activeSongSource.getFirst(), false);
        showNode(songPane);
        updateBackButtonState();
        applySearchFilter();
    }

    private void navigateBack() {
        switch (currentViewState) {
            case ALBUM_DETAIL -> showAlbums();
            case ARTIST_DETAIL -> showArtists();
            case YEAR_DETAIL -> showYears();
            default -> showAllSongs();
        }
    }

    private void updateBackButtonState() {
        boolean showBack = currentViewState == ViewState.ALBUM_DETAIL
                || currentViewState == ViewState.ARTIST_DETAIL
                || currentViewState == ViewState.YEAR_DETAIL;
        backLink.setManaged(showBack);
        backLink.setVisible(showBack);
    }

    private void updateSelectedPlaylist() {
        if (selectedPlaylist == null) {
            playlistHeader.setText("playlist songs");
            playlistHelper.setText("Create a playlist to start building your queue collections.");
            playlistSongItems.clear();
            return;
        }
        playlistHeader.setText(selectedPlaylist.getName());
        playlistHelper.setText("Use play playlist (or double-click playlist) to start. Right-click songs to remove/add.");
        playbackService.setQueue(selectedPlaylist.getSongs(), "Playlist: " + selectedPlaylist.getName(),
                selectedPlaylist.getSongs().isEmpty() ? null : selectedPlaylist.getSongs().getFirst(), false);
        applySearchFilter();
    }

    private void playSelectedPlaylist() {
        if (selectedPlaylist == null) {
            showInfo("No playlist selected.");
            return;
        }
        if (selectedPlaylist.getSongs().isEmpty()) {
            showInfo("Selected playlist is empty.");
            return;
        }

        playbackService.setQueue(
                selectedPlaylist.getSongs(),
                "Playlist: " + selectedPlaylist.getName(),
                selectedPlaylist.getSongs().getFirst(),
                true);
    }

    private void refreshPlaylistState() {
        playlistListView.refresh();
        updateSelectedPlaylist();
        if (currentViewState == ViewState.MEMORY) {
            refreshMemoryView();
        }
    }

    private void createPlaylist() {
        TextInputDialog dialog = new TextInputDialog();
        styleDialog(dialog);
        dialog.setTitle("Create playlist");
        dialog.setHeaderText("Create a new playlist");
        dialog.setContentText("Playlist name:");
        Optional<String> result = dialog.showAndWait();
        result.map(String::trim)
                .filter(value -> !value.isEmpty())
                .ifPresent(name -> {
                    Playlist playlist = new Playlist(name);
                    allPlaylists.add(playlist);
                    allPlaylists.sort(Comparator.comparing(Playlist::getName, String.CASE_INSENSITIVE_ORDER));
                    persistPlaylists();
                    applySearchFilter();
                    playlistListView.getSelectionModel().select(playlist);
                    refreshPlaylistState();
                });
    }

    private void renamePlaylist() {
        if (selectedPlaylist == null) {
            showInfo("No playlist selected.");
            return;
        }
        TextInputDialog dialog = new TextInputDialog(selectedPlaylist.getName());
        styleDialog(dialog);
        dialog.setTitle("Rename playlist");
        dialog.setHeaderText("Rename playlist");
        dialog.setContentText("New name:");
        dialog.showAndWait()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .ifPresent(name -> {
                    selectedPlaylist.rename(name);
                    allPlaylists.sort(Comparator.comparing(Playlist::getName, String.CASE_INSENSITIVE_ORDER));
                    persistPlaylists();
                    refreshPlaylistState();
                });
    }

    private void deletePlaylist() {
        if (selectedPlaylist == null) {
            showInfo("No playlist selected.");
            return;
        }
        Playlist playlistToDelete = selectedPlaylist;
        allPlaylists.remove(playlistToDelete);
        selectedPlaylist = null;
        playlistListView.getSelectionModel().clearSelection();
        persistPlaylists();
        refreshPlaylistState();
    }

    private void refreshMemoryView() {
        allMemoryRows.clear();
        allMemoryRows.add("This month");
        analyticsService.getTopSongs(java.time.LocalDate.now().withDayOfMonth(1), java.time.LocalDate.now(), 10)
                .forEach(stats -> allMemoryRows.add(formatStatsLine(stats)));
        allMemoryRows.add("This year");
        analyticsService.getTopSongs(java.time.LocalDate.now().withDayOfYear(1), java.time.LocalDate.now(), 10)
                .forEach(stats -> allMemoryRows.add(formatStatsLine(stats)));
        if (!libraryService.getWarnings().isEmpty()) {
            allMemoryRows.add("Warnings");
            allMemoryRows.addAll(libraryService.getWarnings());
        }
        if (allMemoryRows.size() <= 2) {
            allMemoryRows.add("No qualified plays yet. Plays appear here after 30 seconds of listening.");
        }
        memoryItems.setAll(allMemoryRows);
    }

    private String formatStatsLine(SongStats stats) {
        Song song = songsById.get(stats.songId());
        String songLabel = song == null ? stats.songId() : song.getDisplayLine();
        return "%s | plays=%d | seconds=%d".formatted(songLabel, stats.playCount(), stats.totalPlayTimeSeconds());
    }

    private void applySearchFilter() {
        String needle = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        switch (currentViewState) {
            case ALL_SONGS, QUEUE, ALBUM_DETAIL, ARTIST_DETAIL, YEAR_DETAIL -> songItems.setAll(activeSongSource.stream()
                    .filter(song -> needle.isBlank() || song.toSearchableText().contains(needle))
                    .toList());
            case ALBUMS -> albumItems.setAll(allAlbums.stream()
                    .filter(album -> needle.isBlank() || (album.name() + " " + album.artistName()).toLowerCase().contains(needle))
                    .toList());
            case ARTISTS -> artistItems.setAll(allArtists.stream()
                    .filter(artist -> needle.isBlank() || artist.name().toLowerCase().contains(needle))
                    .toList());
            case YEARS -> yearItems.setAll(allYears.stream()
                .filter(year -> needle.isBlank() || year.year().toLowerCase().contains(needle))
                .toList());
            case PLAYLISTS -> {
                playlistItems.setAll(allPlaylists.stream()
                        .filter(playlist -> needle.isBlank() || playlist.getName().toLowerCase().contains(needle)
                                || playlist.getSongs().stream().anyMatch(song -> song.toSearchableText().contains(needle)))
                        .sorted(Comparator.comparing(Playlist::getName, String.CASE_INSENSITIVE_ORDER))
                        .toList());
                playlistListView.refresh();
                if (selectedPlaylist != null && playlistItems.contains(selectedPlaylist)) {
                    playlistListView.getSelectionModel().select(selectedPlaylist);
                }
                if (selectedPlaylist == null) {
                    playlistSongItems.clear();
                } else {
                    playlistSongItems.setAll(selectedPlaylist.getSongs().stream()
                            .filter(song -> needle.isBlank() || song.toSearchableText().contains(needle))
                            .toList());
                }
            }
            case MEMORY -> memoryItems.setAll(allMemoryRows.stream()
                    .filter(line -> needle.isBlank() || line.toLowerCase().contains(needle))
                    .toList());
        }
        songTable.setItems(songItems);
        playlistSongTable.setItems(playlistSongItems);
    }

    private void loadLibrary() {
        librarySongs.clear();
        librarySongs.addAll(libraryService.loadLibrary());
        songsById.clear();
        librarySongs.forEach(song -> songsById.put(song.getId(), song));

        allAlbums.clear();
        allAlbums.addAll(libraryService.getAlbums(librarySongs));
        allArtists.clear();
        allArtists.addAll(libraryService.getArtists(librarySongs));
        allYears.clear();
        allYears.addAll(librarySongs.stream()
            .collect(java.util.stream.Collectors.groupingBy(Song::getDisplayYear))
            .entrySet().stream()
            .map(entry -> new YearBucket(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(YearBucket::year))
            .toList());

        allPlaylists.clear();
        allPlaylists.addAll(playlistPersistenceService.loadPlaylists(songsById));

        albumItems.setAll(allAlbums);
        artistItems.setAll(allArtists);
        yearItems.setAll(allYears);
        playlistItems.setAll(allPlaylists);
        songItems.setAll(librarySongs);
        updateOnboardingState();
    }

    private void persistPlaylists() {
        playlistPersistenceService.savePlaylists(allPlaylists);
    }

    private void updateOnboardingState() {
        boolean showOnboarding = librarySongs.isEmpty();
        onboardingPane.setManaged(showOnboarding);
        onboardingPane.setVisible(showOnboarding);
        onboardingHelper.setText("Drop MP3/WAV files into: " + libraryService.getMusicDirectory());
    }

    private void openMusicFolder() {
        try {
            if (!Desktop.isDesktopSupported()) {
                showInfo("Desktop integration is not available. Open this folder manually:\n" + libraryService.getMusicDirectory());
                return;
            }
            Desktop.getDesktop().open(libraryService.getMusicDirectory().toFile());
        } catch (IOException exception) {
            showInfo("Could not open music folder automatically. Open manually:\n" + libraryService.getMusicDirectory());
        }
    }

    private void rescanLibrary() {
        loadLibrary();
        switch (currentViewState) {
            case ALBUMS -> showAlbums();
            case ARTISTS -> showArtists();
            case YEARS -> showYears();
            case PLAYLISTS -> showPlaylists();
            case MEMORY -> showMemory();
            default -> showAllSongs();
        }
    }

    private void cycleThemeMode() {
        themeMode = themeMode.nextManualMode();
        updateThemeLabel();
        applyTheme();
    }

    private void updateThemeLabel() {
        ThemeMode resolved = themeService.resolve(themeMode);
        themeLabel.set("theme: " + themeMode.name().toLowerCase() + " [" + resolved.name().toLowerCase() + "]");
    }

    private void startThemeRefreshLoop() {
        themeRefreshTimeline = new Timeline(new KeyFrame(Duration.minutes(10), event -> {
            updateThemeLabel();
            if (themeMode == ThemeMode.AUTO) {
                applyTheme();
            }
        }));
        themeRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        themeRefreshTimeline.play();
    }

    private void applyTheme() {
        if (scene == null) {
            return;
        }
        String stylesheet = themeService.stylesheetFor(themeMode);
        String external = getClass().getResource(stylesheet).toExternalForm();
        scene.getStylesheets().setAll(external);
    }

    private void showNode(javafx.scene.Node node) {
        centerStack.getChildren().forEach(child -> child.setVisible(child == node));
        centerStack.getChildren().forEach(child -> child.setManaged(child == node));
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        styleDialog(alert);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void styleDialog(Dialog<?> dialog) {
        if (scene != null && scene.getWindow() != null) {
            dialog.initOwner(scene.getWindow());
        }

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStyleClass().add("app-dialog");

        String stylesheet = themeService.stylesheetFor(themeMode);
        java.net.URL resource = getClass().getResource(stylesheet);
        if (resource != null) {
            dialogPane.getStylesheets().setAll(resource.toExternalForm());
        }
    }

    private enum ViewState {
        ALL_SONGS,
        ALBUMS,
        ARTISTS,
        YEARS,
        PLAYLISTS,
        QUEUE,
        MEMORY,
        ALBUM_DETAIL,
        ARTIST_DETAIL,
        YEAR_DETAIL
    }

    private record YearBucket(String year, List<Song> songs) {
    }

    @FunctionalInterface
    private interface SongListSupplier {
        List<Song> get();
    }

    @FunctionalInterface
    private interface StringSupplier {
        String get();
    }
}
