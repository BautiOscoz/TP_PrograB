package GUI;

import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;


import java.util.List;


import static GUI.RankingTableFactory.*;

/** Displays a ranking table in the shared ranking panel. */
public final class RankingPanelView {
    private final VBox rankingPanel;
    private final Label phaseLabel;
    private final Label zoneTitleLabel;
    private final Runnable preparePanel;

    public RankingPanelView(VBox rankingPanel, Label phaseLabel, Label zoneTitleLabel, Runnable preparePanel) {
        this.rankingPanel = rankingPanel;
        this.phaseLabel = phaseLabel;
        this.zoneTitleLabel = zoneTitleLabel;
        this.preparePanel = preparePanel;
    }

    @SafeVarargs
    public final <T> TableView<T> showRanking(
            String title, List<T> rows, TableColumn<T, ?>... columns) {
        preparePanel.run();
        phaseLabel.setText(title);
        zoneTitleLabel.setVisible(false);
        TableView<T> table = createTable(rows, columns);
        Label summary = new Label(rows.size() + " entries");
        summary.getStyleClass().add("ranking-summary");
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        rankingPanel.getChildren().setAll(summary, table);
        rankingPanel.setVisible(true);
        rankingPanel.setManaged(true);
        return table;
    }
}
