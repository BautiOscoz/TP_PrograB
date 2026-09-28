package Controller;

import Core.Run.Championship;
import Core.domain.City;
import Core.domain.Stadium;
import Core.persistance.ChampionshipRepository;
import Infrastructure.database.CityRepository;
import Infrastructure.database.StadiumRepository;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;


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
            List<City> cities = new CityRepository().findAll(
                    championship.getCountries()
            );
            List<Stadium> stadiums = new StadiumRepository().findAll(cities);
            championship.loadVenues(cities, stadiums);
        } catch (SQLException | IllegalArgumentException | IllegalStateException exception) {
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

        rootPane.getChildren().setAll(view);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("/MainMenu.fxml")
            );
            rootPane.getChildren().setAll(view);
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
