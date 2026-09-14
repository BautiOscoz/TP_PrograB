package Controller;

import Core.Run.Championship;
import Core.persistance.ChampionshipRepository;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.event.ActionEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

import java.io.IOException;

public class MainMenuController {

    @FXML private AnchorPane rootPane;

    @FXML
    private void handleNewTournament(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/NewTournament.fxml"));
            Parent view = loader.load();
            rootPane.getChildren().setAll(view);
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLoadTournament(ActionEvent event) {
        ChampionshipRepository repository = new ChampionshipRepository();

        if (!repository.exists()) {
            showError("There is no saved tournament.");
            return;
        }

        Championship championship = repository.load();

        if (championship == null) {
            showError("The saved tournament could not be loaded.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Tournament.fxml"));
            Parent view = loader.load();

            TournamentController controller = loader.getController();
            controller.setTournament(championship, "Loaded Tournament");

            rootPane.getChildren().setAll(view);
        } catch (IOException | IllegalStateException e) {
            showError("Could not open the tournament.");
        }
    }

    @FXML
    private void handleStats(ActionEvent event) {
        System.out.println("Stats - pendiente");
    }

    @FXML
    private void handleExit(ActionEvent event) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle("Tournament");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
