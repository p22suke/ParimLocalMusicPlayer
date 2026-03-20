package ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Reusable horizontal tab bar used by listView and unitView.
 */
public final class TabBar extends ScrollPane {
    private final HBox tabRow = new HBox(6);
    private final Consumer<String> onTabSelected;
    private final Consumer<String> onTabClosed;
    private final Runnable onAddTab;
    private final String addTabLabel;

    public TabBar(Consumer<String> onTabSelected) {
        this(onTabSelected, null, null, "+");
    }

    public TabBar(Consumer<String> onTabSelected,
                  Consumer<String> onTabClosed,
                  Runnable onAddTab,
                  String addTabLabel) {
        this.onTabSelected = onTabSelected;
        this.onTabClosed = onTabClosed;
        this.onAddTab = onAddTab;
        this.addTabLabel = addTabLabel == null || addTabLabel.isBlank() ? "+" : addTabLabel;
        getStyleClass().add("tab-bar");
        tabRow.getStyleClass().add("tab-row");
        tabRow.setPadding(new Insets(0));

        setContent(tabRow);
        setFitToHeight(true);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setVbarPolicy(ScrollBarPolicy.NEVER);
        setPannable(true);
        setFocusTraversable(false);
    }

    public void setTabs(List<TabItem> tabs, String activeKey) {
        tabRow.getChildren().clear();
        for (TabItem tab : tabs) {
            Button button = new Button(tab.title());
            button.getStyleClass().add("tab-button");
            if (tab.key().equals(activeKey)) {
                button.getStyleClass().add("tab-active");
            }
            button.setOnAction(event -> onTabSelected.accept(tab.key()));
            button.setFocusTraversable(false);

            if (!tab.closable() || onTabClosed == null) {
                tabRow.getChildren().add(button);
                continue;
            }

            Button closeButton = new Button("×");
            closeButton.getStyleClass().add("tab-close-button");
            closeButton.setFocusTraversable(false);
            closeButton.setOnAction(event -> onTabClosed.accept(tab.key()));

            StackPane tabContainer = new StackPane(button, closeButton);
            tabContainer.getStyleClass().add("tab-container");
            StackPane.setMargin(closeButton, new Insets(0, 4, 0, 0));
            closeButton.setTranslateX(42);
            tabRow.getChildren().add(tabContainer);
        }

        if (onAddTab != null) {
            Button addButton = new Button(addTabLabel);
            addButton.getStyleClass().add("tab-add-button");
            addButton.setFocusTraversable(false);
            addButton.setOnAction(event -> onAddTab.run());
            tabRow.getChildren().add(addButton);
        }
    }

    public record TabItem(String key, String title, boolean closable) {
        public TabItem(String key, String title) {
            this(key, title, false);
        }
    }
}
