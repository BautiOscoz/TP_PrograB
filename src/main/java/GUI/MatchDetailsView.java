package GUI;

import Core.Run.Championship;
import Core.domain.Match;
import Core.report.MatchDetailsFormatter;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;


import java.util.List;
import java.util.Objects;



/** Displays recorded match lineups, incidents and penalties. */
public final class MatchDetailsView {
    private final AnchorPane tournamentPane;
    private final DialogService dialogs;

    public MatchDetailsView(AnchorPane tournamentPane, DialogService dialogs) {
        this.tournamentPane = tournamentPane;
        this.dialogs = dialogs;
    }
    public void show(Championship championship, List<Match> matches) {
        if (matches.isEmpty()) {
            dialogs.showInformation("Match details", "Play a match to view its details.");
            return;
        }
        MatchDetailsFormatter formatter = new MatchDetailsFormatter();
        ComboBox<Match> selector = new ComboBox<>();
        selector.getItems().setAll(matches);
        selector.setMaxWidth(Double.MAX_VALUE);
        selector.setConverter(new StringConverter<>() {
            @Override public String toString(Match match) {
                return match == null ? "" : formatter.title(match);
            }
            @Override public Match fromString(String value) {
                throw new UnsupportedOperationException("Select a recorded match.");
            }
        });
        TextArea timeline = new TextArea();
        timeline.getStyleClass().add("report-area");
        timeline.setEditable(false);
        timeline.setWrapText(true);
        timeline.setPrefSize(720, 480);
        selector.valueProperty().addListener((observable, previous, selected) -> {
            if (selected != null) {
                timeline.setText(formatter.format(championship, selected));
                timeline.positionCaret(0);
            }
        });
        selector.getSelectionModel().selectLast();
        VBox content = new VBox(10, new Label("Select a played match"), selector, timeline);
        VBox.setVgrow(timeline, javafx.scene.layout.Priority.ALWAYS);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Match details and history");
        alert.setHeaderText("Starting lineups, match incidents and penalty shootouts");
        alert.setResizable(true);
        if (tournamentPane.getScene() != null) {
            alert.initOwner(tournamentPane.getScene().getWindow());
        }
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        alert.getDialogPane().getStyleClass().add("match-dialog");
        alert.showAndWait();
    }
}
