package ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.text.Text;
import model.Song;

import java.util.function.Function;

/**
 * Shared table rendering helpers for the narrow terminal-like layout.
 */
public final class TableCellRenderer {
    private TableCellRenderer() {
    }

    public static TableColumn<Song, String> createSongColumn(String title, Function<Song, String> mapper, double widthRatio) {
        TableColumn<Song, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(mapper.apply(cellData.getValue())));
        column.setCellFactory(ignored -> new WrappingCell());
        column.setReorderable(false);
        column.setSortable(true);
        column.setPrefWidth(100 * widthRatio);
        return column;
    }

    private static final class WrappingCell extends TableCell<Song, String> {
        private final Text text = new Text();

        private WrappingCell() {
            setAlignment(Pos.TOP_LEFT);
            text.wrappingWidthProperty().bind(widthProperty().subtract(16));
            setGraphic(text);
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                text.setText(null);
                setGraphic(null);
            } else {
                text.setText(item);
                setGraphic(text);
            }
        }
    }
}
