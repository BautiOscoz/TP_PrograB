package GUI;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;


import java.util.List;

import javafx.scene.control.TableCell;


/** Builds consistently styled ranking tables and columns. */
public final class RankingTableFactory {
    private RankingTableFactory() {}
    public static <T, V> TableColumn<T, V> rankingColumn(
            String title,
            java.util.function.Function<T, V> value) {

        TableColumn<T, V> column = new TableColumn<>(title);

        column.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(
                        value.apply(cell.getValue())
                )
        );

        column.setMinWidth(100);
        column.setPrefWidth(180);

        column.setSortable(false);
        column.setReorderable(false);
        column.setResizable(false);

        return column;
    }

    public static <T> TableColumn<T, Double> decimalRankingColumn(
            String title,
            java.util.function.Function<T, Double> value,
            String format) {

        TableColumn<T, Double> column = rankingColumn(title, value);

        column.setCellFactory(col -> new TableCell<T, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);

                setText(empty || item == null
                        ? null
                        : String.format(format, item));
            }
        });

        return column;
    }
    @SafeVarargs
    public static <T> TableView<T> createTable(List<T> rows, TableColumn<T, ?>... columns) {
        TableView<T> table = new TableView<>();
        table.getItems().setAll(rows);
        for (TableColumn<T, ?> column : columns) table.getColumns().add(column);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("ranking-table");
        table.setPlaceholder(new Label("No results yet. Simulate matches to see the ranking."));
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        return table;
    }

}
