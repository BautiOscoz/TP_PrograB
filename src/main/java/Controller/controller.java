package Controller;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class controller implements Initializable {

    @FXML private AnchorPane rootPane;
    @FXML private ImageView backgroundImage;
    @FXML private StackPane btn1, btn2, btn3, btn4;
    @FXML private Circle circle1, circle2, circle3, circle4;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Backround image resize
        if (backgroundImage != null) {
            backgroundImage.setPreserveRatio(false);
            backgroundImage.fitWidthProperty().bind(rootPane.widthProperty());
            backgroundImage.fitHeightProperty().bind(rootPane.heightProperty());
        }

        // Scene minimal size
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((winObs, oldWin, newWin) -> {
                    if (newWin instanceof Stage) {
                        Stage stage = (Stage) newWin;
                        stage.setMinWidth(600);
                        stage.setMinHeight(400);
                    }
                });
            }
        });

        StackPane[] buttons = {btn1, btn2, btn3, btn4};
        Circle[] circles = {circle1, circle2, circle3, circle4};

        for (int i = 0; i < buttons.length; i++) {
            StackPane btn = buttons[i];
            Circle circle = circles[i];

            circle.setManaged(false);

            btn.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
                circle.setCenterX(newBounds.getWidth() / 2.0);
                circle.setCenterY(newBounds.getHeight() / 2.0);
            });

            Rectangle clipRect = new Rectangle();
            clipRect.setArcWidth(12);
            clipRect.setArcHeight(12);
            btn.setClip(clipRect);

            btn.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
                clipRect.setWidth(newBounds.getWidth());
                clipRect.setHeight(newBounds.getHeight());
            });
        }
    }

    private Circle getCircleFromButton(StackPane btn) {
        if (btn == btn1) return circle1;
        if (btn == btn2) return circle2;
        if (btn == btn3) return circle3;
        if (btn == btn4) return circle4;
        return null;
    }

    // Btn actions

    @FXML
    private void handleNewTournament(MouseEvent event) {
        System.out.println("Iniciando Nuevo Torneo...");
    }

    @FXML
    private void handleLoadTournament(MouseEvent event) {
        System.out.println("Cargando Torneo...");
    }

    @FXML
    private void handleStats(MouseEvent event) {
        System.out.println("Abriendo Estadísticas...");
    }

    @FXML
    private void handleExit(MouseEvent event) {
        Stage stage = (Stage) rootPane.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }

    // Animations

    @FXML
    private void handleMouseEntered(MouseEvent event) {
        StackPane targetBtn = (StackPane) event.getSource();
        Circle targetCircle = getCircleFromButton(targetBtn);

        if (targetCircle != null) {
            ScaleTransition scaleAnimation = new ScaleTransition(Duration.millis(400), targetCircle);
            scaleAnimation.setInterpolator(Interpolator.SPLINE(0, 0, 0.2, 1));
            scaleAnimation.setFromX(targetCircle.getScaleX());
            scaleAnimation.setFromY(targetCircle.getScaleY());
            scaleAnimation.setToX(1);
            scaleAnimation.setToY(1);
            scaleAnimation.playFromStart();

            Label label = (Label) targetBtn.getChildren().get(2);
            label.setStyle("-fx-text-fill: #0d1b2a;");
        }
    }

    @FXML
    private void handleMouseExited(MouseEvent event) {
        StackPane targetBtn = (StackPane) event.getSource();
        Circle targetCircle = getCircleFromButton(targetBtn);

        if (targetCircle != null) {
            ScaleTransition scaleAnimation = new ScaleTransition(Duration.millis(400), targetCircle);
            scaleAnimation.setInterpolator(Interpolator.SPLINE(0, 0, 0.2, 1));
            scaleAnimation.setFromX(targetCircle.getScaleX());
            scaleAnimation.setFromY(targetCircle.getScaleY());
            scaleAnimation.setToX(0);
            scaleAnimation.setToY(0);
            scaleAnimation.playFromStart();

            Label label = (Label) targetBtn.getChildren().get(2);
            label.setStyle("-fx-text-fill: #fef08a;");
        }
    }

    @FXML
    private void handleMousePressed(MouseEvent event) {
        StackPane targetBtn = (StackPane) event.getSource();
        targetBtn.setScaleX(0.96);
        targetBtn.setScaleY(0.96);
    }

    @FXML
    private void handleMouseReleased(MouseEvent event) {
        StackPane targetBtn = (StackPane) event.getSource();
        targetBtn.setScaleX(1.0);
        targetBtn.setScaleY(1.0);
    }
}