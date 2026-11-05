package GUI;

import Controller.TournamentController;
import Core.Run.Championship;
import Core.domain.City;
import Core.domain.Stadium;
import Core.enums.TournamentStage;
import Core.loader.VenueLoader;
import Core.report.TournamentReportService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Executable JavaFX integration test. Run from an isolated working directory containing torneo.json. */
public class TournamentModularizationTest {
    public static void main(String[] args) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.startup(() -> { });
        Platform.runLater(() -> {
            try { verify(); } catch (Throwable error) { failure.set(error); }
            finally { done.countDown(); }
        });
        done.await();
        Platform.exit();
        if (failure.get() != null) throw new AssertionError("UI integration failed", failure.get());
        System.out.println("Tournament modularization integration checks passed.");
    }

    @SuppressWarnings("unchecked")
    private static void verify() throws Exception {
        Championship tournament = new Championship("torneo.json", 2026L);
        City city = new City(1L, "Test City", tournament.getCountries().get(0));
        List<Stadium> stadiums = new ArrayList<>();
        for (long i = 1; i <= 40; i++) stadiums.add(new Stadium(i, "Stadium " + i, city));
        tournament.loadVenues(List.of(city), stadiums);
        tournament.setVenueLoader(countries -> new VenueLoader.Venues(List.of(city), stadiums));

        FXMLLoader loader = new FXMLLoader(TournamentModularizationTest.class.getResource("/Tournament.fxml"));
        Parent root = loader.load();
        Stage stage = new Stage();
        stage.setScene(new Scene(root, 1280, 900));
        TournamentController controller = loader.getController();
        controller.setTournament(tournament, "Integration test");
        stage.show();
        root.applyCss();
        root.layout();
        TableView<?> standings = (TableView<?>) loader.getNamespace().get("standingsTable");
        require(!standings.getItems().isEmpty(), "Initial standings");
        for (TableColumn<?, ?> column : standings.getColumns()) {
            require(!column.isSortable() && !column.isReorderable() && !column.isResizable(), "Fixed standings columns");
        }
        VBox rankings = (VBox) loader.getNamespace().get("rankingPanel");
        for (String id : List.of("scorersBtn", "participationBtn", "fairplayBtn", "teamStBtn", "refereesStBtn")) {
            ((Button) loader.getNamespace().get(id)).fire();
            require(rankings.isVisible() && !standings.isVisible(), "Ranking navigation: " + id);
        }
        ((Button) loader.getNamespace().get("playerStBtn")).fire();
        ComboBox<String> positions = (ComboBox<String>) loader.getNamespace().get("playerPositionFilter");
        ComboBox<PlayerStatsView.PlayerChoice> players = (ComboBox<PlayerStatsView.PlayerChoice>) loader.getNamespace().get("individualPlayerFilter");
        for (String position : List.copyOf(positions.getItems())) {
            positions.setValue(position);
            require(players.getItems().size() > 1, "Players for position " + position);
            players.getSelectionModel().select(1);
            TabPane tabs = (TabPane) rankings.getChildren().getLast();
            TableView<?> table = (TableView<?>) tabs.getTabs().getFirst().getContent();
            require(table.getItems().size() == 1, "Individual player filter");
        }
        TabPane playerTabs = (TabPane) rankings.getChildren().getLast();
        TableView<TournamentReportService.PlayerStatsRow> detailsTable =
                (TableView<TournamentReportService.PlayerStatsRow>) playerTabs.getTabs().getFirst().getContent();
        TableRow<TournamentReportService.PlayerStatsRow> row = detailsTable.getRowFactory().call(detailsTable);
        row.updateTableView(detailsTable);
        row.updateIndex(0);
        var click = new javafx.scene.input.MouseEvent(
                javafx.scene.input.MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
                javafx.scene.input.MouseButton.PRIMARY, 1,
                false, false, false, false, false, false, false, false, false, true, null);
        row.getOnMouseClicked().handle(click);
        require(playerTabs.getTabs().size() == 2, "Player detail tab opens");
        row.getOnMouseClicked().handle(click);
        require(playerTabs.getTabs().size() == 2, "Existing player tab is reused");
        ((Button) loader.getNamespace().get("groupBtn")).fire();
        require(standings.isVisible() && !rankings.isVisible(), "Return to groups");
        int guard = 0;
        while (tournament.hasPendingGroupMatchdays() && guard++ < 30) {
            ((Button) loader.getNamespace().get("simulateMatchdayBtn")).fire();
            waitForSimulation(root);
        }
        require(!tournament.hasPendingGroupMatchdays(), "Group simulation completes");
        int scheduled = tournament.getMatches().size();
        tournament.scheduleQuarterFinalsIfNeeded();
        require(tournament.getMatches().size() == scheduled, "Scheduling is idempotent");
        ((Button) loader.getNamespace().get("knockoutBtn")).fire();
        guard = 0;
        while (tournament.getStage() != TournamentStage.FINISHED && guard++ < 60) {
            ((Button) loader.getNamespace().get("simulateKnockoutBtn")).fire();
            waitForSimulation(root);
        }
        require(tournament.getStage() == TournamentStage.FINISHED, "Knockout simulation completes");
        Label winner = (Label) loader.getNamespace().get("winnerBanner");
        require(!winner.getText().equals("WINNER: -"), "Winner banner");
        ((Button) loader.getNamespace().get("playerStBtn")).fire();
        positions.setValue("All");
        players.getSelectionModel().selectFirst();
        TabPane tabs = (TabPane) rankings.getChildren().getLast();
        TableView<?> playerTable = (TableView<?>) tabs.getTabs().getFirst().getContent();
        require(playerTable.getItems().size() == new TournamentReportService().getPlayerStats(tournament).size(), "Reports after simulation");
        stage.hide();
    }

    private static void waitForSimulation(Parent root) {
        if (!root.isDisabled()) return;
        Object key = new Object();
        javafx.beans.value.ChangeListener<Boolean> listener = (obs, before, disabled) -> {
            if (!disabled) Platform.runLater(() -> Platform.exitNestedEventLoop(key, true));
        };
        javafx.animation.PauseTransition timeout = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(20));
        timeout.setOnFinished(event -> Platform.exitNestedEventLoop(key, false));
        root.disabledProperty().addListener(listener);
        timeout.play();
        Object completed = Platform.enterNestedEventLoop(key);
        timeout.stop();
        root.disabledProperty().removeListener(listener);
        require(Boolean.TRUE.equals(completed), "Background simulation finishes");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
