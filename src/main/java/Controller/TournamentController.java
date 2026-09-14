package Controller;

import Core.Run.Championship;
import Core.domain.FinalMatch;
import Core.domain.Match;
import Core.domain.SecondLegMatch;
import Core.domain.Team;
import Core.incidents.PenaltyShootout;
import Core.domain.TeamStanding;
import Core.domain.TournamentZone;
import Core.persistance.ChampionshipRepository;
import Core.report.TournamentReportService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
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
import java.time.LocalDate;
import java.util.List;

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
    @FXML private VBox knockoutBracket;
    @FXML private HBox bracketContent;
    @FXML private VBox quarterColumn;
    @FXML private VBox quarterConnectorColumn;
    @FXML private VBox semiColumn;
    @FXML private VBox semiConnectorColumn;
    @FXML private VBox finalColumn;
    @FXML private Label winnerBanner;
    @FXML private Button groupBtn;
    @FXML private Button knockoutBtn;
    @FXML private Button scorersBtn;
    @FXML private Button participationBtn;
    @FXML private Button fairplayBtn;
    @FXML private Button teamStBtn;
    @FXML private Button playerStBtn;
    @FXML private Button refereesStBtn;
    @FXML private Button getIDBtn;
    @FXML private Button backBtn;

    private final TournamentReportService reportService = new TournamentReportService();
    private final ChampionshipRepository repository = new ChampionshipRepository();
    private Championship championship;
    private String tournamentName;

    @FXML
    private void initialize() {
        configureColumns();
        reportArea.setEditable(false);
        reportArea.setWrapText(true);
        showGroupTable();
        showKnockoutBracket(false);
    }

    public void setTournament(Championship championship, String tournamentName) {
        this.championship = championship;
        this.tournamentName = tournamentName;
        tournamentNameLabel.setText(tournamentName);
        configureCloseSave();
        showGroupStage();
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

        try {
            if (!hasKnockoutMatches()) {
                List<Core.domain.Team> quarterFinalWinners =
                        championship.simulateQuarterFinals(LocalDate.now().plusDays(7));

                List<Core.domain.Team> finalists =
                        championship.simulateSemiFinals(
                                quarterFinalWinners,
                                LocalDate.now().plusDays(21)
                        );

                championship.simulateFinal(
                        finalists,
                        LocalDate.now().plusDays(35)
                );

                repository.save(championship);
            }

            phaseLabel.setText("Knockout Stage");
            zoneTitleLabel.setVisible(false);
            showKnockoutBracket(true);
            buildKnockoutBracket();
        } catch (IllegalArgumentException | IllegalStateException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleTopScorers() {
        phaseLabel.setText("Top Scorers");
        zoneTitleLabel.setVisible(false);
        showReport();

        StringBuilder text = new StringBuilder();
        reportService.getTopScorers(championship).forEach(row ->
                text.append(row.getPlayer().getName()).append(" ")
                        .append(row.getPlayer().getLastName())
                        .append(" - ")
                        .append(row.getTeam().getName())
                        .append(" - Goals: ")
                        .append(row.getGoals())
                        .append(" - Penalty goals: ")
                        .append(row.getPenaltyGoals())
                        .append("\n")
        );
        reportArea.setText(text.toString());
    }

    @FXML
    private void handleParticipation() {
        phaseLabel.setText("Participation Ranking");
        zoneTitleLabel.setVisible(false);
        showReport();

        StringBuilder text = new StringBuilder();
        reportService.getParticipation(championship).forEach(row ->
                text.append(row.getPlayer().getName()).append(" ")
                        .append(row.getPlayer().getLastName())
                        .append(" - ")
                        .append(row.getTeam().getName())
                        .append(" - Matches: ")
                        .append(row.getMatches())
                        .append(" - Minutes: ")
                        .append(row.getMinutes())
                        .append("\n")
        );
        reportArea.setText(text.toString());
    }

    @FXML
    private void handleFairPlay() {
        phaseLabel.setText("Fair Play");
        zoneTitleLabel.setVisible(false);
        showReport();

        StringBuilder text = new StringBuilder();
        reportService.getFairPlay(championship).forEach(row ->
                text.append(row.getTeam().getName())
                        .append(" - Yellow: ")
                        .append(row.getYellow())
                        .append(" - Red: ")
                        .append(row.getRed())
                        .append(" - Points: ")
                        .append(row.getPoints())
                        .append("\n")
        );
        reportArea.setText(text.toString());
    }

    @FXML
    private void handleTeamsStats() {
        phaseLabel.setText("Teams Stats");
        zoneTitleLabel.setVisible(false);
        showReport();

        StringBuilder text = new StringBuilder();
        reportService.getTeamStats(championship).forEach(row ->
                text.append(row.getTeam().getName())
                        .append(" - Avg age: ")
                        .append(String.format("%.1f", row.getAverageAge()))
                        .append(" - Coach: ")
                        .append(row.getCoach())
                        .append(" - GF: ")
                        .append(row.getGoalsFor())
                        .append(" - GA: ")
                        .append(row.getGoalsAgainst())
                        .append(" - Effectiveness: ")
                        .append(String.format("%.1f%%", row.getEffectiveness()))
                        .append("\n")
        );
        reportArea.setText(text.toString());
    }

    @FXML
    private void handlePlayersStats() {
        phaseLabel.setText("Players Stats");
        zoneTitleLabel.setVisible(false);
        showReport();

        StringBuilder text = new StringBuilder();
        reportService.getPlayerStats(championship).forEach(row ->
                text.append(row.getPlayer().getName()).append(" ")
                        .append(row.getPlayer().getLastName())
                        .append(" - ")
                        .append(row.getTeam().getName())
                        .append(" - Position: ")
                        .append(row.getPosition())
                        .append(" - Matches: ")
                        .append(row.getMatches())
                        .append(" - Minutes: ")
                        .append(row.getMinutes())
                        .append(" - Goals: ")
                        .append(row.getGoals())
                        .append("\n")
        );
        reportArea.setText(text.toString());
    }

    @FXML
    private void handleRefereesStats() {
        phaseLabel.setText("Referees Stats");
        zoneTitleLabel.setVisible(false);
        showReport();

        StringBuilder text = new StringBuilder();
        reportService.getRefereeStats(championship).forEach(row ->
                text.append(row.getReferee().getName()).append(" ")
                        .append(row.getReferee().getLastName())
                        .append(" - Matches: ")
                        .append(row.getMatches())
                        .append(" - Years: ")
                        .append(row.getYears())
                        .append("\n")
        );
        reportArea.setText(text.toString());
    }

    @FXML
    private void handleGetIdentifications() {
        showInformation(
                "Get Identifications",
                "This option is ready to be connected to the identification report."
        );
    }

    @FXML
    private void handleBack() {
        saveTournament();

        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("/MainMenu.fxml")
            );
            tournamentPane.getChildren().setAll(view);
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void showGroupStage() {
        phaseLabel.setText("Group Stage");
        zoneTitleLabel.setVisible(true);
        showGroupTable();
        showZone(0);
    }

    private void showGroupTable() {
        standingsTable.setVisible(true);
        standingsTable.setManaged(true);
        reportArea.setVisible(false);
        reportArea.setManaged(false);
        knockoutBracket.setVisible(false);
        knockoutBracket.setManaged(false);
    }

    private void showReport() {
        standingsTable.setVisible(false);
        standingsTable.setManaged(false);
        reportArea.setVisible(true);
        reportArea.setManaged(true);
        knockoutBracket.setVisible(false);
        knockoutBracket.setManaged(false);
    }

    private void showKnockoutBracket(boolean visible) {
        standingsTable.setVisible(false);
        standingsTable.setManaged(false);
        reportArea.setVisible(false);
        reportArea.setManaged(false);
        knockoutBracket.setVisible(visible);
        knockoutBracket.setManaged(visible);
    }

    private void showZone(int zoneIndex) {
        if (championship == null) {
            return;
        }

        List<TournamentZone> zones = championship.getTournamentZones();
        if (zoneIndex < 0 || zoneIndex >= zones.size()) {
            return;
        }

        TournamentZone zone = zones.get(zoneIndex);
        zoneTitleLabel.setText(zone.getName());
        standingsTable.getItems().setAll(
                championship.getStandings(zone)
        );
    }

    private boolean hasKnockoutMatches() {
        return championship.getMatches().stream()
                .anyMatch(match -> !match.isGroupStage());
    }

    private void configureColumns() {
        positionColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(
                        standingsTable.getItems().indexOf(cell.getValue()) + 1
                )
        );
        teamColumn.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(
                        cell.getValue().getTeam().getName()
                )
        );
        pointsColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getPoints())
        );
        playedColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getPlayed())
        );
        wonColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getWon())
        );
        drawnColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getDrawn())
        );
        lostColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getLost())
        );
        goalsForColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getGoalsFor())
        );
        goalsAgainstColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getGoalsAgainst())
        );
        differenceColumn.setCellValueFactory(cell ->
                new ReadOnlyObjectWrapper<>(cell.getValue().getGoalDifference())
        );
    }

    private void buildKnockoutBracket() {
        quarterColumn.getChildren().clear();
        quarterConnectorColumn.getChildren().clear();
        semiColumn.getChildren().clear();
        semiConnectorColumn.getChildren().clear();
        finalColumn.getChildren().clear();
        winnerBanner.setText("GANADOR: -");

        List<Match> knockoutMatches = championship.getPlayedMatches().stream()
                .filter(match -> !match.isGroupStage())
                .toList();

        addSeriesCards(quarterColumn, knockoutMatches, 0, 4);
        addSeriesCards(semiColumn, knockoutMatches, 8, 2);
        addConnectors(quarterConnectorColumn, 4);
        addConnectors(semiConnectorColumn, 2);

        if (knockoutMatches.size() >= 13) {
            Match finalMatch = knockoutMatches.get(12);
            Team winner = getMatchWinner(finalMatch);
            finalColumn.getChildren().add(createMatchCard(List.of(finalMatch), winner));
            winnerBanner.setText("GANADOR: " + winner.getName());
        }
    }

    private void addConnectors(VBox column, int count) {
        for (int i = 0; i < count; i++) {
            Label arrow = new Label("›");
            arrow.getStyleClass().add("bracket-arrow");
            column.getChildren().add(arrow);
        }
    }

    private void addSeriesCards(VBox column, List<Match> knockoutMatches, int startIndex, int count) {
        for (int i = 0; i < count; i++) {
            int index = startIndex + (i * 2);
            if (index + 1 >= knockoutMatches.size()) {
                return;
            }
            Match firstLeg = knockoutMatches.get(index);
            Match secondLeg = knockoutMatches.get(index + 1);
            Team winner = getSeriesWinner(firstLeg, secondLeg);
            column.getChildren().add(createMatchCard(List.of(firstLeg, secondLeg), winner));
        }
    }

    private VBox createMatchCard(List<Match> matches, Team winner) {
        Match displayMatch = matches.size() == 1 ? matches.get(0) : matches.get(matches.size() - 1);

        VBox card = new VBox(3);
        card.getStyleClass().add("match-card");
        card.setFillWidth(true);

        addTeamScoreRow(card, displayMatch.getHomeTeam(), displayMatch.getHomeGoals(), winner);
        addTeamScoreRow(card, displayMatch.getAwayTeam(), displayMatch.getAwayGoals(), winner);

        if (isSettledByPenalties(displayMatch)) {
            int[] penalties = getPenaltyScore(displayMatch);
            Label penaltyLabel = new Label("Penales " + penalties[0] + " - " + penalties[1]);
            penaltyLabel.getStyleClass().add("penalty-score");
            penaltyLabel.setMaxWidth(Double.MAX_VALUE);
            penaltyLabel.setAlignment(javafx.geometry.Pos.CENTER);
            card.getChildren().add(penaltyLabel);
        }

        if (matches.size() == 2) {
            int[] aggregate = getAggregateScore(matches.get(0), matches.get(1));
            Label aggregateLabel = new Label("Global " + aggregate[0] + " - " + aggregate[1]);
            aggregateLabel.getStyleClass().add("aggregate-score");
            aggregateLabel.setMaxWidth(Double.MAX_VALUE);
            aggregateLabel.setAlignment(javafx.geometry.Pos.CENTER);
            card.getChildren().add(aggregateLabel);
        }

        Label winnerLabel = new Label("GANADOR: " + winner.getName());
        winnerLabel.getStyleClass().add("match-winner");
        winnerLabel.setMaxWidth(Double.MAX_VALUE);
        winnerLabel.setAlignment(javafx.geometry.Pos.CENTER);
        card.getChildren().add(winnerLabel);
        return card;
    }

    private void addTeamScoreRow(VBox card, Team team, int goals, Team winner) {
        HBox row = new HBox(6);
        row.getStyleClass().add("team-score-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label teamLabel = new Label(team.getName());
        teamLabel.getStyleClass().add("match-team");
        teamLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(teamLabel, javafx.scene.layout.Priority.ALWAYS);

        Label scoreLabel = new Label(String.valueOf(goals));
        scoreLabel.getStyleClass().add("match-score");
        scoreLabel.setMinWidth(24);
        scoreLabel.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        if (winner.equals(team)) {
            teamLabel.getStyleClass().add("match-team-winner");
            scoreLabel.getStyleClass().add("match-score-winner");
        }

        row.getChildren().addAll(teamLabel, scoreLabel);
        card.getChildren().add(row);
    }

    private int[] getAggregateScore(Match firstLeg, Match secondLeg) {
        int homeAggregate = firstLeg.getHomeGoals() + secondLeg.getAwayGoals();
        int awayAggregate = firstLeg.getAwayGoals() + secondLeg.getHomeGoals();
        return new int[]{homeAggregate, awayAggregate};
    }

    private boolean isSettledByPenalties(Match match) {
        boolean flagged = false;
        if (match instanceof SecondLegMatch secondLeg) {
            flagged = secondLeg.isSettledByPenalties();
        } else if (match instanceof FinalMatch finalMatch) {
            flagged = finalMatch.isSettledByPenalties();
        }

        if (flagged) {
            return true;
        }

        if (match.getHomeGoals() != match.getAwayGoals()) {
            return false;
        }

        return match.getIncidents().stream()
                .anyMatch(incident -> incident instanceof PenaltyShootout);
    }

    private Team getSeriesWinner(Match firstLeg, Match secondLeg) {
        int[] aggregate = getAggregateScore(firstLeg, secondLeg);
        if (aggregate[0] > aggregate[1]) {
            return firstLeg.getHomeTeam();
        }
        if (aggregate[1] > aggregate[0]) {
            return firstLeg.getAwayTeam();
        }
        return getPenaltyWinner(secondLeg);
    }

    private Team getMatchWinner(Match match) {
        if (match.getHomeGoals() > match.getAwayGoals()) {
            return match.getHomeTeam();
        }
        if (match.getAwayGoals() > match.getHomeGoals()) {
            return match.getAwayTeam();
        }
        return getPenaltyWinner(match);
    }

    private Team getPenaltyWinner(Match match) {
        int[] penalties = getPenaltyScore(match);
        return penalties[0] > penalties[1] ? match.getHomeTeam() : match.getAwayTeam();
    }

    private int[] getPenaltyScore(Match match) {
        int home = 0;
        int away = 0;

        if (match.getHomeLineup() == null || match.getAwayLineup() == null) {
            return new int[]{0, 0};
        }

        for (var incident : match.getIncidents()) {
            if (!(incident instanceof PenaltyShootout penalty)) {
                continue;
            }
            if (!penalty.isScored()) {
                continue;
            }
            if (match.getHomeLineup().getPlayers().contains(penalty.getPlayer())) {
                home++;
            } else if (match.getAwayLineup().getPlayers().contains(penalty.getPlayer())) {
                away++;
            }
        }

        return new int[]{home, away};
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
        saveTournament();
    }

    private void saveTournament() {
        if (championship != null) {
            repository.save(championship);
        }
    }

    private void showInformation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Tournament");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
