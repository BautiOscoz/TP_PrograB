package GUI;

import javafx.scene.control.Alert;





/** Displays application information and error dialogs. */
public final class DialogService {
    public void showInformation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Tournament");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
