package meik;

import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import mudelid.Song;

import java.util.List;
import java.util.function.Consumer;

/**
 * listView pane (context selector): fixed tabs + selectable list items.
 */
public final class ListViewPane extends VBox {
    private final TabBar tabBar;
    private final HBox playlistActions = new HBox(10);
    private final ListView<ListItem> contentList = new ListView<>();

    public ListViewPane(Consumer<String> onTabSelected,
            Consumer<ListItem> onItemOpened,
            Runnable onCreatePlaylist,
            Runnable onRenamePlaylist,
            Runnable onDeletePlaylist) {
        getStyleClass().addAll("section-pane", "list-pane");
        setSpacing(6);
        setPadding(new Insets(8));

        tabBar = new TabBar(onTabSelected);

        Hyperlink createLink = new Hyperlink("new playlist");
        createLink.setOnAction(event -> onCreatePlaylist.run());
        Hyperlink renameLink = new Hyperlink("rename playlist");
        renameLink.setOnAction(event -> onRenamePlaylist.run());
        Hyperlink deleteLink = new Hyperlink("delete playlist");
        deleteLink.setOnAction(event -> onDeletePlaylist.run());
        playlistActions.getStyleClass().add("playlist-actions");
        playlistActions.getChildren().addAll(createLink, renameLink, deleteLink);

        contentList.getStyleClass().add("list-content");
        contentList.setPlaceholder(new javafx.scene.control.Label("No items for this tab."));
        contentList.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 1) {
                ListItem selected = contentList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    onItemOpened.accept(selected);
                }
            }
        });
        contentList.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                ListItem selected = contentList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    onItemOpened.accept(selected);
                }
            }
        });

        VBox.setVgrow(contentList, Priority.ALWAYS);
        getChildren().addAll(tabBar, playlistActions, contentList);
    }

    public void setTabs(List<TabBar.TabItem> tabs, String activeKey) {
        tabBar.setTabs(tabs, activeKey);
    }

    public void setItems(List<ListItem> items) {
        contentList.getItems().setAll(items);
    }

    public ListView<ListItem> getContentList() {
        return contentList;
    }

    public void showPlaylistActions(boolean visible) {
        playlistActions.setManaged(visible);
        playlistActions.setVisible(visible);
    }

    public ListItem getSelectedItem() {
        return contentList.getSelectionModel().getSelectedItem();
    }

    public record ListItem(String key, String title, String label, String searchable, List<Song> songs) {
    }
}
