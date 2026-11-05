package Controller;

import Core.Run.Championship;
import GUI.*;
import static Core.report.MatchScoreSummary.getPenaltyScore;
import Core.domain.Match;
import Core.domain.TeamStanding;
import Core.persistance.ChampionshipRepository;
import Core.report.TournamentReportService;
import javafx.scene.control.ComboBox;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;


import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javafx.concurrent.Task;

import Core.enums.TournamentStage;
import java.util.Comparator;

public class TournamentController {

    @FXML private AnchorPane tournamentPane;
    @FXML private Label tournamentNameLabel;
    @FXML private Label phaseLabel;
    @FXML private Label zoneTitleLabel;
    @FXML private TableView<TeamStanding> standingsTable;
    @FXML private TableColumn<TeamStanding, Number> positionColumn;
    @FXML private TableColumn<TeamStanding, String> teamColumn;
    @FXML private TableColumn<TeamStanding, Number> pointsColumn;
    @FXML private TableColumn<TeamStanding, Number> playedColumn;
    @FXML private TableColumn<TeamStanding, Number> wonColumn;
    @FXML private TableColumn<TeamStanding, Number> drawnColumn;
    @FXML private TableColumn<TeamStanding, Number> lostColumn;
    @FXML private TableColumn<TeamStanding, Number> goalsForColumn;
    @FXML private TableColumn<TeamStanding, Number> goalsAgainstColumn;
    @FXML private TableColumn<TeamStanding, Number> differenceColumn;
    @FXML private TextArea reportArea;
    @FXML private VBox rankingPanel;
    @FXML private javafx.scene.control.ScrollPane knockoutScroll;
    @FXML private VBox quarterColumn;
    @FXML private VBox quarterConnectorColumn;
    @FXML private VBox semiColumn;
    @FXML private VBox semiConnectorColumn;
    @FXML private VBox finalColumn;
    @FXML private Label winnerBanner;
    @FXML private Button simulateMatchdayBtn;
    @FXML private Button simulateKnockoutBtn;
    @FXML private TextArea matchdayResultsArea;
    @FXML private HBox playerFilterBar;
    @FXML private ComboBox<String> playerPositionFilter;
    @FXML private ComboBox<PlayerStatsView.PlayerChoice> individualPlayerFilter;
    private final TournamentReportService reportService = new TournamentReportService();
    private final ChampionshipRepository repository;
    private final DialogService dialogs = new DialogService();
    private GroupStageView groupStageView;
    private RankingsView rankingsView;
    private PlayerStatsView playerStatsView;
    private KnockoutBracketView knockoutBracketView;
    private MatchDetailsView matchDetailsView;
    private IdentificationExportController identificationExportController;
    private Championship championship;
    private String tournamentName;
    private boolean operationInProgress;

    public TournamentController() { this(new ChampionshipRepository()); }

    TournamentController(ChampionshipRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @FXML
    private void initialize() {
        RankingPanelView rankingView = new RankingPanelView(
                rankingPanel, phaseLabel, zoneTitleLabel,
                () -> showPanels(false, false, false));
        rankingsView = new RankingsView(rankingPanel, rankingView, reportService);
        playerStatsView = new PlayerStatsView(rankingPanel, playerFilterBar,
                playerPositionFilter, individualPlayerFilter, rankingView, reportService);
        groupStageView = new GroupStageView(zoneTitleLabel,
                phaseLabel,
                standingsTable,
                positionColumn,
                teamColumn,
                pointsColumn,
                playedColumn,
                wonColumn,
                drawnColumn,
                lostColumn,
                goalsForColumn,
                goalsAgainstColumn,
                differenceColumn,
                matchdayResultsArea);
        matchDetailsView = new MatchDetailsView(tournamentPane, dialogs);
        knockoutBracketView = new KnockoutBracketView(quarterColumn,
                quarterConnectorColumn,
                semiColumn,
                semiConnectorColumn,
                finalColumn,
                winnerBanner,
                matchDetailsView);
        identificationExportController = new IdentificationExportController(
                tournamentPane, reportArea, this::showReport, dialogs, this::setOperationInProgress);
        zoneTitleLabel.managedProperty().bind(zoneTitleLabel.visibleProperty());
        reportArea.setEditable(false);
        reportArea.setWrapText(true);
        showGroupTable();
    }

    public void setTournament(Championship championship, String tournamentName) {
        this.championship = championship;
        championship.scheduleQuarterFinalsIfNeeded();
        this.tournamentName = tournamentName;
        tournamentNameLabel.setText(tournamentName);
        configureCloseSave();
        showGroupStage();
        updateSimulationButtons();
    }

    @FXML
    private void handleSimulateMatchday() {
        if (operationInProgress || championship == null || !championship.hasPendingGroupMatchdays()) {
            return;
        }
        int playedMatchday = championship.getCurrentMatchday();
        runSimulation("Simulating matchday " + playedMatchday + "...", () -> {
            championship.simulateNextMatchday();
            championship.scheduleQuarterFinalsIfNeeded();
            return playedMatchday;
        }, matchday -> {
            showGroupStage();
            phaseLabel.setText("Group Stage - Matchday " + matchday
                    + (championship.getCurrentMatchday() == matchday ? " has postponed matches" : " completed"));
        }, this::showGroupStage);
    }

    private <T> void runSimulation(String message, Callable<T> simulation,
                                   Consumer<T> onSuccess, Runnable onFailure) {
        if (operationInProgress) return;
        setOperationInProgress(true);
        phaseLabel.setText(message);
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return simulation.call();
            }
        };
        // Task completion handlers run on the JavaFX application thread.
        task.setOnSucceeded(event -> finishSimulation(() -> onSuccess.accept(task.getValue())));
        task.setOnFailed(event -> finishSimulation(() -> {
            onFailure.run();
            Throwable failure = task.getException();
            showError(failure.getMessage() == null ? failure.toString() : failure.getMessage());
        }));
        Thread worker = new Thread(task, "tournament-simulation");
        worker.setDaemon(true);
        worker.start();
    }

    private void finishSimulation(Runnable refresh) {
        try {
            // The worker has finished; saving cannot observe a partially changing tournament.
            saveTournament();
            refresh.run();
        } finally {
            setOperationInProgress(false);
        }
    }

    private void setOperationInProgress(boolean running) {
        operationInProgress = running;
        // Block navigation, reports and editing while the worker owns the tournament.
        tournamentPane.setDisable(running);
        if (!running && championship != null) updateSimulationButtons();
    }

    private void updateSimulationButtons() {
        simulateMatchdayBtn.setDisable(!championship.hasPendingGroupMatchdays());
        simulateKnockoutBtn.setDisable(
                championship.getStage() == TournamentStage.GROUP_STAGE
                        || championship.getStage() == TournamentStage.FINISHED);
        simulateKnockoutBtn.setText(
                championship.hasPendingPenaltyShootout()
                        ? "Simulate Penalties"
                        : "Simulate Next Match"
        );
    }

    @FXML
    private void handleGroupStage() {
        showGroupStage();
    }

    @FXML
    private void handleZoneA() {
        showZone(0);
    }

    @FXML
    private void handleZoneB() {
        showZone(1);
    }

    @FXML
    private void handleZoneC() {
        showZone(2);
    }

    @FXML
    private void handleZoneD() {
        showZone(3);
    }

    @FXML
    private void handleKnockoutStage() {
        if (championship == null) {
            return;
        }

        phaseLabel.setText("Knockout Stage");
        zoneTitleLabel.setVisible(false);
        showKnockoutBracket(true);
        buildKnockoutBracket();
    }

    @FXML
    private void handleSimulateKnockoutMatch() {
        if (operationInProgress || championship == null
                || championship.getStage() == TournamentStage.GROUP_STAGE
                || championship.getStage() == TournamentStage.FINISHED) {
            return;
        }
        boolean wasPenaltyShootoutPending = championship.hasPendingPenaltyShootout();
        runSimulation(wasPenaltyShootoutPending ? "Simulating penalties..." : "Simulating match...",
                championship::simulateNextKnockoutMatch, played -> {
            if (!played.isPlayed()) {
                phaseLabel.setText(championship.getKnockoutDecisionCriterion(played));
            } else if (played.isForfeit() && !wasPenaltyShootoutPending) {
                phaseLabel.setText(played.getAdministrativeReason());
            } else if (wasPenaltyShootoutPending) {
                int[] penalties = getPenaltyScore(played);
                phaseLabel.setText(
                        "Penalties: " + played.getHomeTeam().getName()
                                + " " + penalties[0] + " - " + penalties[1]
                                + " " + played.getAwayTeam().getName()
                                + " | " + championship.getKnockoutDecisionCriterion(played)
                );
            } else {
                phaseLabel.setText(
                        played.getHomeTeam().getName()
                                + " " + played.getHomeGoals()
                                + " - " + played.getAwayGoals()
                                + " " + played.getAwayTeam().getName()
                                + " | Stadium: " + played.getStadium().getName()
                                + " | City: "
                                + played.getStadium().getCity().getName()
                                + " | " + championship.getKnockoutDecisionCriterion(played)
                );
            }
            zoneTitleLabel.setVisible(false);
            showKnockoutBracket(true);
            buildKnockoutBracket();
        }, this::handleKnockoutStage);
    }

    @FXML
    private void handleTopScorers() {
        rankingsView.showTopScorers(championship);
    }

    @FXML
    private void handleParticipation() {
        rankingsView.showParticipation(championship);
    }

    @FXML
    private void handleFairPlay() {
        rankingsView.showFairPlay(championship);
    }

    @FXML
    private void handleTeamsStats() {
        rankingsView.showTeamsStats(championship);
    }

    @FXML
    private void handlePlayersStats() {
        playerStatsView.show(championship);
    }

    @FXML
    private void handleRefereesStats() {
        rankingsView.showRefereesStats(championship);
    }

    private void setPlayerFilterVisible(boolean visible) {
        playerFilterBar.setVisible(visible);
        playerFilterBar.setManaged(visible);
    }

    @FXML
    private void handleGetIdentifications() {
        if (operationInProgress) return;
        identificationExportController.export(championship, tournamentName);
    }

    @FXML
    private void handleBack() {
        if (operationInProgress || !saveTournament()) return;

        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("/MainMenu.fxml")
            );
            tournamentPane.getScene().setRoot(view);
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void showGroupStage() {
        showZone(0);
    }

    private void showGroupTable() {
        showPanels(true, false, false);
    }

    private void showReport() {
        showPanels(false, true, false);
    }

    private void showKnockoutBracket(boolean visible) {
        showPanels(false, false, visible);
    }

    private void showPanels(boolean groups, boolean report, boolean knockout) {
        setPanelVisible(rankingPanel, false);
        setPlayerFilterVisible(false);
        setPanelVisible(standingsTable, groups);
        setPanelVisible(matchdayResultsArea, groups);
        setPanelVisible(reportArea, report);
        setPanelVisible(knockoutScroll, knockout);
    }

    private void setPanelVisible(javafx.scene.Node panel, boolean visible) {
        panel.setVisible(visible);
        panel.setManaged(visible);
    }

    private void showZone(int zoneIndex) {
        showGroupTable();
        groupStageView.showZone(championship, zoneIndex);
    }

    private void buildKnockoutBracket() {
        knockoutBracketView.show(championship);
    }

    @FXML
    private void handleVenuesCrud() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Venues.fxml"));
            Parent view = loader.load();
            VenuesController controller = loader.getController();
            controller.setTournament(championship, tournamentName);
            tournamentPane.getScene().setRoot(view);
        } catch (IOException | IllegalStateException exception) {
            showError("The cities and stadiums screen could not be opened: "
                    + exception.getMessage());
        }
    }

    private void showMatchDetails(List<Match> matches) {
        matchDetailsView.show(championship, matches);
    }

    @FXML
    private void handleMatchHistory() {
        if (championship == null) return;
        showMatchDetails(championship.getPlayedMatches().stream()
                .sorted(Comparator.comparing(Match::getMatchDate)).toList());
    }

    private void configureCloseSave() {
        if (tournamentPane.getScene() == null) {
            tournamentPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    setWindowSaveHandler();
                }
            });
        } else {
            setWindowSaveHandler();
        }
    }

    private void setWindowSaveHandler() {
        if (tournamentPane.getScene().getWindow() != null) {
            tournamentPane.getScene().getWindow().setOnCloseRequest(this::handleWindowClose);
            return;
        }

        tournamentPane.getScene().windowProperty().addListener(
                (obs, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        newWindow.setOnCloseRequest(this::handleWindowClose);
                    }
                }
        );
    }

    private void handleWindowClose(WindowEvent event) {
        if (operationInProgress) {
            event.consume();
            phaseLabel.setText("Please wait for the current operation to finish before closing.");
            return;
        }
        if (!saveTournament()) event.consume();
    }

    private boolean saveTournament() {
        try {
            if (championship != null) repository.save(championship);
            return true;
        } catch (IllegalStateException exception) {
            showError(exception.getMessage()
                    + "\nYour latest changes are still in memory. Keep this window open and retry after fixing the save location.");
            return false;
        }
    }

    private void showError(String message) {
        dialogs.showError(message);
    }

}
