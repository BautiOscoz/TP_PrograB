package Controller;

import Core.Run.Championship;
import Core.persistance.ChampionshipRepository;
import Infrastructure.database.DatabaseVenueLoader;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;


public class NewTournamentController {

    private static final String DATA_PATH = "torneo.json";

    @FXML private AnchorPane rootPane;
    @FXML private TextField tournamentNameField;

    @FXML
    private void handleCreateTournament(ActionEvent event) {
        String tournamentName = tournamentNameField.getText().trim();

        if (tournamentName.isEmpty()) {
            showError("Tournament name is required.");
            return;
        }

        try {
            Championship championship = new Championship(DATA_PATH);
            loadVenues(championship);
            new ChampionshipRepository().save(championship);
            openTournament(championship, tournamentName);
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            showError(e.getMessage());
        }
    }

    private void loadVenues(Championship championship) {
        try {
            var venues = new DatabaseVenueLoader().load(championship.getCountries());
            championship.loadVenues(venues.cities(), venues.stadiums());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            showWarning(
                    "The tournament was created, but cities and stadiums could not be loaded: "
                            + exception.getMessage()
            );
        }
    }

    private void openTournament(
            Championship championship,
            String tournamentName
    ) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/Tournament.fxml")
        );
        Parent view = loader.load();

        TournamentController controller = loader.getController();
        controller.setTournament(championship, tournamentName);

        rootPane.getScene().setRoot(view);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("/MainMenu.fxml")
            );
            rootPane.getScene().setRoot(view);
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Tournament");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("PostgreSQL");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
