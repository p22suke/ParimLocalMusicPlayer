package ui;

import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Top floor navigation uses text links only, matching the requested terminal
 * style.
 */
public final class NavigationBar extends VBox {
    public enum Section {
        ALL_SONGS,
        ALBUMS,
        ARTISTS,
        YEARS,
        PLAYLISTS,
        QUEUE,
        MEMORY
    }

    public NavigationBar(Consumer<Section> navigateHandler, Runnable themeToggleHandler, StringProperty themeLabel) {
        getStyleClass().addAll("section-pane", "nav-bar");
        setSpacing(4);
        setPadding(new Insets(10));

        getChildren().add(createLink("all songs", () -> navigateHandler.accept(Section.ALL_SONGS)));
        getChildren().add(createLink("albums", () -> navigateHandler.accept(Section.ALBUMS)));
        getChildren().add(createLink("artists", () -> navigateHandler.accept(Section.ARTISTS)));
        getChildren().add(createLink("years", () -> navigateHandler.accept(Section.YEARS)));
        getChildren().add(createLink("playlists", () -> navigateHandler.accept(Section.PLAYLISTS)));
        getChildren().add(createLink("queue", () -> navigateHandler.accept(Section.QUEUE)));
        getChildren().add(createLink("memory", () -> navigateHandler.accept(Section.MEMORY)));

        Hyperlink themeLink = createLink("theme", themeToggleHandler);
        themeLink.textProperty().bind(themeLabel);
        VBox.setVgrow(themeLink, Priority.ALWAYS);
        getChildren().add(themeLink);
    }

    private Hyperlink createLink(String text, Runnable action) {
        Hyperlink link = new Hyperlink(text);
        link.setOnAction(event -> action.run());
        link.setFocusTraversable(false);
        return link;
    }
}
