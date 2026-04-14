package meik;

import javafx.geometry.Insets;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import mudelid.Song;

import java.util.List;
import java.util.function.Consumer;

/**
 * unitView pane (playback surface): tab contexts + song table.
 */
public final class UnitViewPane extends VBox {
    private final TabBar tabBar;
    private final TableView<Song> songTable;

    public UnitViewPane(TableView<Song> songTable,
            Consumer<String> onTabSelected,
            Consumer<String> onTabClosed,
            Runnable onCreateTab) {
        this.songTable = songTable;

        getStyleClass().addAll("section-pane", "unit-pane");
        setSpacing(6);
        setPadding(new Insets(8));

        tabBar = new TabBar(onTabSelected, onTabClosed, onCreateTab, "+ playlist tab");

        VBox.setVgrow(songTable, Priority.ALWAYS);
        getChildren().addAll(tabBar, songTable);
    }

    public void setTabs(List<TabBar.TabItem> tabs, String activeKey) {
        tabBar.setTabs(tabs, activeKey);
    }

    public TableView<Song> getSongTable() {
        return songTable;
    }
}
