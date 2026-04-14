package meik;

//java enda meigikott
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
// meie mudelid
import mudelid.Album;
import mudelid.Artist;
import mudelid.Playlist;
import mudelid.Song;
import mudelid.ThemeMode;
import service.AnalyticsService;
import service.LibraryService;
import service.MetadataService;
import service.PlaybackService;
import service.PlaylistPersistenceService;
import service.ThemeService;
// klassikaline java kirjandus
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * - global search
 * - listView (tabs + content)
 * - unitView (tabs + content)
 * - existing player bar
 */

public final class MainView extends VBox {
    // konstandid on alati javas CAPS_LOCK
    private static final String ALL_SONGS_KEY = "all-songs";
    private static final String PLAYING_FROM_PREFIX = "Playing from: ";
    private static final String LIKED_SONGS_NAME = "Liked Music";
    // kutsume esinema meie enda mudelite servicesid
    private final LibraryService libraryService;
    private final PlaybackService playbackService;
    private final AnalyticsService analyticsService;
    private final ThemeService themeService;
    private final PlaylistPersistenceService playlistPersistenceService;
    private final MetadataService metadataService;
    // java meigikotist on meil vaja:
    private final TextField searchField = new TextField();
    private final Button dayNightButton = new Button();
    private final ListViewPane listViewPane;
    private final UnitViewPane unitViewPane;
    // mis on observableList? i dont know. vastab copilot: see on vist mingi java
    // eriline list, mis oskab vaadata,
    // millal me sinna midagi lisame või sealt eemaldame, ja siis vastavalt
    // reageerida. nagu mingi vaate ja andmete
    // vahelise sünkroonimise tööriist. me kasutame seda selleks, et kui me muudame
    // mingit laulu nime või lisame uue laulu, siis see muutus kajastuks kohe meie
    // kasutajaliideses, ilma et me peaksime kogu vaadet uuesti joonistama.
    private final ObservableList<Song> unitSongItems = FXCollections.observableArrayList();

    // meie mudelid!!
    private final List<Song> librarySongs = new ArrayList<>();
    private final List<Album> allAlbums = new ArrayList<>();
    private final List<Artist> allArtists = new ArrayList<>();
    private final List<Playlist> allPlaylists = new ArrayList<>();
    private final Map<String, Song> songsById = new LinkedHashMap<>();

    private final Map<String, SongListContext> contextsByKey = new LinkedHashMap<>();

    private ListTab activeListTab = ListTab.PLAYLISTS;
    private SongListContext activeUnitContext;
    private ThemeMode themeMode = ThemeMode.DAY;
    private Scene scene;

    public MainView(LibraryService libraryService,
            PlaybackService playbackService,
            AnalyticsService analyticsService,
            ThemeService themeService,
            PlaylistPersistenceService playlistPersistenceService,
            MetadataService metadataService) {
        this.libraryService = libraryService;
        this.playbackService = playbackService;
        this.analyticsService = analyticsService;
        this.themeService = themeService;
        this.playlistPersistenceService = playlistPersistenceService;
        this.metadataService = metadataService;

        getStyleClass().add("app-root");
        setSpacing(0);
        setFillWidth(true);

        TableView<Song> unitSongTable = createSongTable();
        this.listViewPane = new ListViewPane(
                this::onListTabSelected,
                this::openContextFromListItem,
                () -> createPlaylist(true),
                this::renameSelectedPlaylist,
                this::deleteSelectedPlaylist);
        this.unitViewPane = new UnitViewPane(
                unitSongTable,
                this::onUnitTabSelected,
                this::closeUnitTab,
                () -> createPlaylist(true));

        installSongTableInteractions(unitSongTable);

        VBox middleFloor = new VBox(8);
        middleFloor.getStyleClass().addAll("section-pane", "middle-floor");
        middleFloor.setStyle("-fx-padding: 10;");

        searchField.setPromptText("search list and units");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> applySearchFilter());

        dayNightButton.getStyleClass().add("day-night-button");
        dayNightButton.setOnAction(event -> toggleDayNight());
        updateDayNightButtonLabel();

        HBox searchRow = new HBox(8, searchField, dayNightButton);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox.setVgrow(listViewPane, Priority.ALWAYS);
        VBox.setVgrow(unitViewPane, Priority.ALWAYS);
        middleFloor.getChildren().addAll(searchRow, listViewPane, unitViewPane);

        PlayerBar playerBar = new PlayerBar(playbackService);
        middleFloor.prefHeightProperty().bind(heightProperty().multiply(0.90));
        playerBar.prefHeightProperty().bind(heightProperty().multiply(0.10));

        getChildren().addAll(middleFloor, playerBar);

        configureListViewCellFactory();
        loadLibrary();
        initialiseContexts();
        refreshTabs();
        applySearchFilter();

        playbackService.queueSnapshotProperty()
                .addListener((ListChangeListener<? super Song>) change -> syncQueueToActiveUnit());
        playbackService.currentContextProperty()
                .addListener((observable, oldValue, newValue) -> syncQueueToActiveUnit());
    }

    public void attachScene(Scene scene) {
        this.scene = scene;
        applyTheme();
    }

    public void shutdown() {
        playbackService.dispose();
    }

    private void configureListViewCellFactory() {
        listViewPane.getContentList().setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ListViewPane.ListItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });
    }

    private TableView<Song> createSongTable() {
        TableView<Song> table = new TableView<>();
        table.getStyleClass().add("song-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No songs in this context."));
        table.getColumns().add(TableCellRenderer.createSongColumn("laul", song -> song.getTitle(), 0.28));
        table.getColumns().add(TableCellRenderer.createSongColumn("kunstnik", song -> song.getArtist(), 0.20));
        table.getColumns().add(TableCellRenderer.createSongColumn("album", song -> song.getAlbum(), 0.38));
        table.getColumns().add(TableCellRenderer.createSongColumn("aasta", song -> song.getYear(), 0.14));
        return table;
    }

    private void installSongTableInteractions(TableView<Song> table) {
        table.setRowFactory(view -> {
            TableRow<Song> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    playSong(row.getItem());
                }
            });
            return row;
        });

        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                Song selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    playSong(selected);
                }
            }
        });

        table.setOnContextMenuRequested(event -> {
            Song selected = table.getSelectionModel().getSelectedItem();
            if (selected == null || activeUnitContext == null) {
                return;
            }
            javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();

            javafx.scene.control.MenuItem playNextItem = new javafx.scene.control.MenuItem("play next");
            playNextItem.setOnAction(actionEvent -> playbackService.addSongToPlayNext(
                    selected,
                    activeUnitContext.songs(),
                    activeUnitContext.name()));
            contextMenu.getItems().add(playNextItem);

            javafx.scene.control.Menu addToPlaylist = new javafx.scene.control.Menu("add to playlist");
            if (allPlaylists.isEmpty()) {
                javafx.scene.control.MenuItem emptyItem = new javafx.scene.control.MenuItem("no playlists yet");
                emptyItem.setDisable(true);
                addToPlaylist.getItems().add(emptyItem);
            } else {
                for (Playlist playlist : allPlaylists) {
                    javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(playlist.getName());
                    item.setOnAction(actionEvent -> {
                        playlist.addSong(selected);
                        persistPlaylists();
                        refreshTabs();
                        applySearchFilter();
                    });
                    addToPlaylist.getItems().add(item);
                }
            }
            contextMenu.getItems().add(addToPlaylist);

            Playlist playlistContext = activePlaylistContext();
            if (playlistContext != null) {
                javafx.scene.control.MenuItem removeItem = new javafx.scene.control.MenuItem("remove from playlist");
                removeItem.setOnAction(actionEvent -> {
                    playlistContext.removeSong(selected);
                    persistPlaylists();
                    refreshTabs();
                    applySearchFilter();
                });
                contextMenu.getItems().add(removeItem);
            }

            javafx.scene.control.MenuItem editMetadataItem = new javafx.scene.control.MenuItem("edit metadata");
            editMetadataItem.setOnAction(actionEvent -> openMetadataEditor(selected));
            contextMenu.getItems().add(editMetadataItem);

            contextMenu.show(table, event.getScreenX(), event.getScreenY());
        });
    }

    private Playlist activePlaylistContext() {
        if (activeUnitContext == null || !activeUnitContext.key().startsWith("playlist:")) {
            return null;
        }
        String playlistName = activeUnitContext.key().substring("playlist:".length());
        return allPlaylists.stream()
                .filter(playlist -> playlist.getName().equals(playlistName))
                .findFirst()
                .orElse(null);
    }

    private void onListTabSelected(String tabKey) {
        activeListTab = ListTab.fromKey(tabKey);
        refreshTabs();
        applySearchFilter();
    }

    private void onUnitTabSelected(String contextKey) {
        SongListContext context = contextsByKey.get(contextKey);
        if (context == null) {
            return;
        }
        activeUnitContext = context;
        refreshTabs();
        applySearchFilter();
        playbackService.setQueue(
                activeUnitContext.songs(),
                activeUnitContext.name(),
                activeUnitContext.songs().isEmpty() ? null : activeUnitContext.songs().getFirst(),
                false);
    }

    private void openContextFromListItem(ListViewPane.ListItem listItem) {
        SongListContext context = contextForListItem(listItem);
        contextsByKey.put(context.key(), context);
        activeUnitContext = context;
        refreshTabs();
        applySearchFilter();
        playbackService.setQueue(
                activeUnitContext.songs(),
                activeUnitContext.name(),
                activeUnitContext.songs().isEmpty() ? null : activeUnitContext.songs().getFirst(),
                false);
    }

    private void closeUnitTab(String contextKey) {
        if (ALL_SONGS_KEY.equals(contextKey)) {
            return;
        }
        contextsByKey.remove(contextKey);
        if (activeUnitContext != null && contextKey.equals(activeUnitContext.key())) {
            activeUnitContext = contextsByKey.get(ALL_SONGS_KEY);
        }
        refreshTabs();
        applySearchFilter();
    }

    private SongListContext contextForListItem(ListViewPane.ListItem listItem) {
        List<String> songIds = listItem.songs().stream().map(Song::getId).toList();
        return new SongListContext(listItem.key(), listItem.title(), songIds, List.copyOf(listItem.songs()));
    }

    private void playSong(Song song) {
        if (song == null || activeUnitContext == null || activeUnitContext.songs().isEmpty()) {
            return;
        }
        playbackService.playSelectedSong(song, activeUnitContext.songs(), activeUnitContext.name());
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

        allPlaylists.clear();
        allPlaylists.addAll(playlistPersistenceService.loadPlaylists(songsById));
        allPlaylists.sort(Comparator.comparing(Playlist::getName, String.CASE_INSENSITIVE_ORDER));
    }

    private void initialiseContexts() {
        contextsByKey.clear();
        SongListContext allSongs = new SongListContext(
                ALL_SONGS_KEY,
                "All Songs",
                librarySongs.stream().map(Song::getId).toList(),
                List.copyOf(librarySongs));
        contextsByKey.put(allSongs.key(), allSongs);
        activeUnitContext = allSongs;

        playbackService.setQueue(
                allSongs.songs(),
                allSongs.name(),
                allSongs.songs().isEmpty() ? null : allSongs.songs().getFirst(),
                false);
    }

    private void refreshTabs() {
        listViewPane.setTabs(List.of(
                new TabBar.TabItem(ListTab.PLAYLISTS.key, "Playlists", false),
                new TabBar.TabItem(ListTab.ALBUMS.key, "Albums", false),
                new TabBar.TabItem(ListTab.ARTISTS.key, "Kunstnikud", false)),
                activeListTab.key);
        listViewPane.showPlaylistActions(activeListTab == ListTab.PLAYLISTS);

        List<TabBar.TabItem> unitTabs = contextsByKey.values().stream()
                .map(context -> new TabBar.TabItem(
                        context.key(),
                        context.name(),
                        !ALL_SONGS_KEY.equals(context.key()) && !isLikedSongsKey(context.key())))
                .toList();
        unitViewPane.setTabs(unitTabs, activeUnitContext == null ? ALL_SONGS_KEY : activeUnitContext.key());
    }

    private void applySearchFilter() {
        String needle = searchNeedle();

        List<ListViewPane.ListItem> filteredListItems = buildListItems().stream()
                .filter(item -> needle.isBlank() || item.searchable().contains(needle))
                .toList();
        listViewPane.setItems(filteredListItems);

        if (activeUnitContext == null) {
            unitSongItems.clear();
        } else {
            unitSongItems.setAll(activeUnitContext.songs().stream()
                    .filter(song -> needle.isBlank() || song.toSearchableText().contains(needle))
                    .toList());
        }
        unitViewPane.getSongTable().setItems(unitSongItems);
    }

    private List<ListViewPane.ListItem> buildListItems() {
        return switch (activeListTab) {
            case PLAYLISTS -> allPlaylists.stream()
                    .sorted(Comparator.comparing(this::playlistSortKey, String.CASE_INSENSITIVE_ORDER))
                    .map(playlist -> {
                        int plays = analyticsService.getPlaylistPlayCount(playlist.getName());
                        String label = playlist.getName() + " (songs=" + playlist.getSongs().size() + ", plays=" + plays
                                + ")";
                        String searchable = (playlist.getName() + " " + playlist.getSongs().stream()
                                .map(Song::toSearchableText)
                                .reduce("", (left, right) -> left + " " + right)).toLowerCase();
                        return new ListViewPane.ListItem(
                                "playlist:" + playlist.getName(),
                                playlist.getName(),
                                label,
                                searchable,
                                playlist.getSongs());
                    })
                    .toList();
            case ALBUMS -> allAlbums.stream()
                    .map(album -> {
                        String year = album.year();
                        String yearPart = year.isBlank() ? "" : " · " + year;
                        String label = album.name() + " -- " + album.artistName()
                                + yearPart + " (" + album.songs().size() + " songs)";
                        String searchable = (album.name() + " " + album.artistName() + " " + year).toLowerCase();
                        return new ListViewPane.ListItem(
                                "album:" + album.name() + "@@" + album.artistName(),
                                "Album: " + album.name(),
                                label,
                                searchable,
                                album.songs());
                    })
                    .toList();
            case ARTISTS -> allArtists.stream()
                    .map(artist -> {
                        long albumCount = artist.songs().stream()
                                .map(Song::getAlbum)
                                .distinct()
                                .count();
                        String label = artist.name()
                                + " (" + albumCount + " albums, " + artist.songs().size() + " songs)";
                        String searchable = artist.name().toLowerCase();
                        return new ListViewPane.ListItem(
                                "artist:" + artist.name(),
                                "Kunstnik: " + artist.name(),
                                label,
                                searchable,
                                artist.songs());
                    })
                    .toList();
        };
    }

    private String searchNeedle() {
        if (searchField.getText() == null) {
            return "";
        }
        return searchField.getText().trim().toLowerCase();
    }

    private void createPlaylist(boolean openTabAfterCreate) {
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
                    refreshTabs();
                    applySearchFilter();

                    if (openTabAfterCreate) {
                        SongListContext context = new SongListContext(
                                "playlist:" + playlist.getName(),
                                playlist.getName(),
                                List.of(),
                                List.of());
                        contextsByKey.put(context.key(), context);
                        activeUnitContext = context;
                        refreshTabs();
                        applySearchFilter();
                    }
                });
    }

    private void renameSelectedPlaylist() {
        Playlist playlist = selectedPlaylistFromList();
        if (playlist == null) {
            showInfo("Select a playlist in listView first.");
            return;
        }
        if (isLikedSongsPlaylist(playlist)) {
            showInfo("Liked Music is pinned and cannot be renamed.");
            return;
        }

        String oldKey = "playlist:" + playlist.getName();
        TextInputDialog dialog = new TextInputDialog(playlist.getName());
        styleDialog(dialog);
        dialog.setTitle("Rename playlist");
        dialog.setHeaderText("Rename playlist");
        dialog.setContentText("New name:");

        dialog.showAndWait()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .ifPresent(newName -> {
                    playlist.rename(newName);
                    allPlaylists.sort(Comparator.comparing(Playlist::getName, String.CASE_INSENSITIVE_ORDER));
                    persistPlaylists();

                    SongListContext openContext = contextsByKey.remove(oldKey);
                    if (openContext != null) {
                        SongListContext renamedContext = new SongListContext(
                                "playlist:" + playlist.getName(),
                                playlist.getName(),
                                playlist.getSongs().stream().map(Song::getId).toList(),
                                playlist.getSongs());
                        contextsByKey.put(renamedContext.key(), renamedContext);
                        if (activeUnitContext != null && oldKey.equals(activeUnitContext.key())) {
                            activeUnitContext = renamedContext;
                        }
                    }

                    refreshTabs();
                    applySearchFilter();
                });
    }

    private void deleteSelectedPlaylist() {
        Playlist playlist = selectedPlaylistFromList();
        if (playlist == null) {
            showInfo("Select a playlist in listView first.");
            return;
        }
        if (isLikedSongsPlaylist(playlist)) {
            showInfo("Liked Music is pinned and cannot be deleted.");
            return;
        }

        String removedKey = "playlist:" + playlist.getName();
        allPlaylists.remove(playlist);
        contextsByKey.remove(removedKey);
        if (activeUnitContext != null && removedKey.equals(activeUnitContext.key())) {
            activeUnitContext = contextsByKey.get(ALL_SONGS_KEY);
        }
        persistPlaylists();
        refreshTabs();
        applySearchFilter();
    }

    private Playlist selectedPlaylistFromList() {
        if (activeListTab != ListTab.PLAYLISTS) {
            return null;
        }
        ListViewPane.ListItem selectedItem = listViewPane.getSelectedItem();
        if (selectedItem == null || !selectedItem.key().startsWith("playlist:")) {
            return null;
        }
        String playlistName = selectedItem.key().substring("playlist:".length());
        return allPlaylists.stream()
                .filter(playlist -> playlist.getName().equals(playlistName))
                .findFirst()
                .orElse(null);
    }

    private void persistPlaylists() {
        playlistPersistenceService.savePlaylists(allPlaylists);
        loadLibrary();
        rebuildOpenContextsAfterReload();
    }

    private void rebuildOpenContextsAfterReload() {
        List<String> previouslyOpenKeys = new ArrayList<>(contextsByKey.keySet());
        String activeKey = activeUnitContext == null ? ALL_SONGS_KEY : activeUnitContext.key();

        initialiseContexts();
        for (String key : previouslyOpenKeys) {
            if (ALL_SONGS_KEY.equals(key)) {
                continue;
            }
            ListViewPane.ListItem item = findListItemByKey(key);
            if (item != null) {
                contextsByKey.put(key, contextForListItem(item));
            }
        }

        SongListContext restored = contextsByKey.get(activeKey);
        if (restored != null) {
            activeUnitContext = restored;
        }
    }

    private ListViewPane.ListItem findListItemByKey(String key) {
        for (Playlist playlist : allPlaylists) {
            String playlistKey = "playlist:" + playlist.getName();
            if (playlistKey.equals(key)) {
                return new ListViewPane.ListItem(
                        playlistKey,
                        playlist.getName(),
                        playlist.getName(),
                        playlist.getName().toLowerCase(),
                        playlist.getSongs());
            }
        }
        for (Album album : allAlbums) {
            String albumKey = "album:" + album.name() + "@@" + album.artistName();
            if (albumKey.equals(key)) {
                return new ListViewPane.ListItem(
                        albumKey,
                        "Album: " + album.name(),
                        album.name(),
                        (album.name() + " " + album.artistName()).toLowerCase(),
                        album.songs());
            }
        }
        for (Artist artist : allArtists) {
            String artistKey = "artist:" + artist.name();
            if (artistKey.equals(key)) {
                return new ListViewPane.ListItem(
                        artistKey,
                        "Kunstnik: " + artist.name(),
                        artist.name(),
                        artist.name().toLowerCase(),
                        artist.songs());
            }
        }
        return null;
    }

    private void syncQueueToActiveUnit() {
        if (activeUnitContext == null) {
            return;
        }
        String playbackContextName = playbackContextName();
        if (!activeUnitContext.name().equals(playbackContextName)) {
            return;
        }

        List<Song> queueSongs = List.copyOf(playbackService.queueSnapshotProperty());
        if (queueSongs.isEmpty()) {
            return;
        }

        activeUnitContext = activeUnitContext.withSongs(queueSongs);
        contextsByKey.put(activeUnitContext.key(), activeUnitContext);
        applySearchFilter();
    }

    private String playbackContextName() {
        String raw = playbackService.currentContextProperty().get();
        if (raw == null) {
            return "";
        }
        if (raw.startsWith(PLAYING_FROM_PREFIX)) {
            return raw.substring(PLAYING_FROM_PREFIX.length());
        }
        return raw;
    }

    private void toggleDayNight() {
        themeMode = themeMode == ThemeMode.DAY ? ThemeMode.NIGHT : ThemeMode.DAY;
        updateDayNightButtonLabel();
        applyTheme();
    }

    private void updateDayNightButtonLabel() {
        dayNightButton.setText(themeMode == ThemeMode.DAY ? "DAY" : "NIGHT");
    }

    private void addSongToLikedSongs(Song song) {
        if (song == null) {
            return;
        }

        Playlist likedSongs = findOrCreateLikedSongsPlaylist();
        int beforeSize = likedSongs.getSongs().size();
        likedSongs.addSong(song);

        if (likedSongs.getSongs().size() == beforeSize) {
            showInfo("Already in Liked Music.");
            return;
        }

        persistPlaylists();
        refreshTabs();
        applySearchFilter();
        showInfo("Added to Liked Music.");
    }

    private Playlist findOrCreateLikedSongsPlaylist() {
        for (Playlist playlist : allPlaylists) {
            if (isLikedSongsPlaylist(playlist)) {
                return playlist;
            }
        }

        Playlist likedSongs = new Playlist(LIKED_SONGS_NAME);
        allPlaylists.add(likedSongs);
        allPlaylists.sort(Comparator.comparing(this::playlistSortKey, String.CASE_INSENSITIVE_ORDER));
        return likedSongs;
    }

    private boolean isLikedSongsPlaylist(Playlist playlist) {
        return playlist != null && LIKED_SONGS_NAME.equalsIgnoreCase(playlist.getName());
    }

    private boolean isLikedSongsKey(String contextKey) {
        return ("playlist:" + LIKED_SONGS_NAME).equalsIgnoreCase(contextKey);
    }

    private String playlistSortKey(Playlist playlist) {
        if (isLikedSongsPlaylist(playlist)) {
            return "";
        }
        return playlist.getName();
    }

    private void openMetadataEditor(Song song) {
        Dialog<ButtonType> dialog = new Dialog<>();
        styleDialog(dialog);
        dialog.setTitle("Edit metadata");
        dialog.setHeaderText(song.getFilePath().getFileName() == null
                ? song.getFilePath().toString()
                : song.getFilePath().getFileName().toString());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setStyle("-fx-padding: 10;");

        TextField titleField = new TextField(song.getTitle());
        TextField artistField = new TextField(song.getArtist());
        TextField albumField = new TextField(song.getAlbum());
        TextField yearField = new TextField(song.getYear());

        titleField.setPrefWidth(240);

        grid.add(new Label("title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("artist:"), 0, 1);
        grid.add(artistField, 1, 1);
        grid.add(new Label("album:"), 0, 2);
        grid.add(albumField, 1, 2);
        grid.add(new Label("year:"), 0, 3);
        grid.add(yearField, 1, 3);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) {
                return;
            }
            try {
                metadataService.writeMetadata(
                        song.getFilePath(),
                        titleField.getText(),
                        artistField.getText(),
                        albumField.getText(),
                        yearField.getText());
                loadLibrary();
                rebuildOpenContextsAfterReload();
                refreshTabs();
                applySearchFilter();
            } catch (java.io.IOException e) {
                showInfo("Could not save metadata: " + e.getMessage());
            }
        });
    }

    private void applyTheme() {
        if (scene == null) {
            return;
        }
        String stylesheet = themeService.stylesheetFor(themeMode);
        String external = getClass().getResource(stylesheet).toExternalForm();
        scene.getStylesheets().setAll(external);
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

    private enum ListTab {
        PLAYLISTS("playlists"),
        ALBUMS("albums"),
        ARTISTS("artists");

        private final String key;

        ListTab(String key) {
            this.key = key;
        }

        private static ListTab fromKey(String key) {
            for (ListTab tab : values()) {
                if (tab.key.equals(key)) {
                    return tab;
                }
            }
            return PLAYLISTS;
        }
    }

    private record SongListContext(String key, String name, List<String> songIds, List<Song> songs) {
        private SongListContext withSongs(List<Song> nextSongs) {
            return new SongListContext(
                    key,
                    name,
                    nextSongs.stream().map(Song::getId).toList(),
                    List.copyOf(nextSongs));
        }
    }
}
