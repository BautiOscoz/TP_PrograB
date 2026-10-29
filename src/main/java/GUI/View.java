package GUI;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class View extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainMenu.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1080, 720);
        stage.setTitle("International Clubs Cup");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
