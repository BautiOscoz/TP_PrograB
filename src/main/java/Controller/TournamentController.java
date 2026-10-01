package Controller;

import Core.Run.Championship;
import Core.domain.FinalMatch;
import Core.domain.FirstLegMatch;
import Core.domain.Match;
import Core.domain.Player;
import Core.domain.SecondLegMatch;
import Core.domain.Team;
import Core.incidents.Incident;
import Core.domain.TeamStanding;
import Core.domain.TournamentZone;
import Core.persistance.ChampionshipRepository;
import Core.report.TournamentReportService;
import Core.report.MatchDetailsFormatter;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
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

import Core.report.IdentificationPdfService;
import javafx.concurrent.Task;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import java.io.File;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import Core.enums.TournamentStage;
import java.time.LocalDate;
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
    @FXML private Button venuesBtn;
    @FXML private Button backBtn;
    @FXML private Button simulateMatchdayBtn;
    @FXML private Button simulateKnockoutBtn;
    @FXML private TextArea matchdayResultsArea;

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
        scheduleQuarterFinalsIfNeeded();
        this.tournamentName = tournamentName;
        tournamentNameLabel.setText(tournamentName);
        configureCloseSave();
        showGroupStage();
        updateSimulationButtons();
    }

    @FXML
    private void handleSimulateMatchday() {
        if (championship == null || !championship.hasPendingGroupMatchdays()) {
            return;
        }

        int playedMatchday = championship.getCurrentMatchday();
        championship.simulateNextMatchday();
        scheduleQuarterFinalsIfNeeded();
        repository.save(championship);

        showGroupStage();
        phaseLabel.setText("Group Stage - Matchday " + playedMatchday + " played");
        updateSimulationButtons();
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

    private void showLatestMatchdayResults() {
        int latestMatchday = championship.getPlayedMatches().stream()
                .filter(Match::isGroupStage)
                .mapToInt(Match::getMatchday)
                .max()
                .orElse(0);

        if (latestMatchday == 0) {
            matchdayResultsArea.setText("No matchdays have been played yet.");
            return;
        }

        StringBuilder results = new StringBuilder();
        results.append("MATCHDAY ").append(latestMatchday).append("\n\n");

        championship.getPlayedMatches().stream()
                .filter(Match::isGroupStage)
                .filter(match -> match.getMatchday() == latestMatchday)
                .forEach(match -> results
                        .append(match.getHomeTeam().getName())
                        .append(" ").append(match.getHomeGoals())
                        .append(" - ").append(match.getAwayGoals())
                        .append(" ").append(match.getAwayTeam().getName())
                        .append("\n"));

        matchdayResultsArea.setText(results.toString());
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
        if (championship == null) {
            return;
        }
        try {
            boolean wasPenaltyShootoutPending = championship.hasPendingPenaltyShootout();
            Match played = championship.simulateNextKnockoutMatch();
            repository.save(championship);
            if (wasPenaltyShootoutPending) {
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
                                + " | Estadio: " + played.getStadium().getName()
                                + " | Ciudad: "
                                + played.getStadium().getCity().getName()
                                + " | " + championship.getKnockoutDecisionCriterion(played)
                );
            }
            zoneTitleLabel.setVisible(false);
            showKnockoutBracket(true);
            buildKnockoutBracket();
            updateSimulationButtons();
        } catch (IllegalStateException | IllegalArgumentException e) {
            repository.save(championship);
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

        if (championship == null) {
            showError("Primero cargá un torneo.");
            return;
        }

        // Seleccionar carpeta de fotografías.
        showInformation("Participant photographs",
                "Name each photograph with the person's document number, for example 12345678.jpg. "
                        + "JPG, JPEG and PNG are supported. Missing photos will be reported in the result.");
        DirectoryChooser directoryChooser = new DirectoryChooser();

        directoryChooser.setTitle(
                "Elegí la carpeta de fotografías"
        );

        File photos = directoryChooser.showDialog(
                tournamentPane.getScene().getWindow()
        );

        if (photos == null) {
            return;
        }

        // Seleccionar dónde guardar el PDF.
        FileChooser chooser = new FileChooser();

        chooser.setTitle("Guardar identificaciones");
        chooser.setInitialFileName("identificaciones.pdf");

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF", "*.pdf")
        );

        File selected = chooser.showSaveDialog(
                tournamentPane.getScene().getWindow()
        );

        if (selected == null) {
            return;
        }

        if (!selected.getName()
                .toLowerCase(java.util.Locale.ROOT)
                .endsWith(".pdf")) {

            showError("El nombre del archivo debe terminar en .pdf.");
            return;
        }

        var teams = List.copyOf(championship.getTeams());
        var referees = List.copyOf(championship.getReferees());
        String title = tournamentName;

        // Generar en segundo plano para no bloquear JavaFX.
        Task<IdentificationPdfService.Result> task = new Task<>() {

            @Override
            protected IdentificationPdfService.Result call()
                    throws Exception {

                return new IdentificationPdfService().generate(
                        teams,
                        referees,
                        title,
                        photos.toPath(),
                        selected.toPath()
                );
            }
        };

        getIDBtn.setDisable(true);

        task.setOnSucceeded(event -> {

            getIDBtn.setDisable(false);

            var result = task.getValue();

            showReport();

            String message =
                    "PDF generado: " + selected.getAbsolutePath()
                            + "\nIdentificaciones: " + result.total()
                            + "\nSin fotografía: "
                            + result.missingPhotos().size();

            reportArea.setText(
                    message + "\n\n"
                            + String.join("\n", result.missingPhotos())
            );

            showInformation("Identificaciones", message);
        });

        task.setOnFailed(event -> {

            getIDBtn.setDisable(false);

            showError(
                    "No se pudo generar el PDF: "
                            + task.getException().getMessage()
            );
        });

        new Thread(task, "identificaciones-pdf").start();
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
        showLatestMatchdayResults();
    }

    private void showGroupTable() {
        standingsTable.setVisible(true);
        standingsTable.setManaged(true);
        reportArea.setVisible(false);
        reportArea.setManaged(false);
        knockoutBracket.setVisible(false);
        knockoutBracket.setManaged(false);
        matchdayResultsArea.setVisible(true);
        matchdayResultsArea.setManaged(true);
    }

    private void showReport() {
        standingsTable.setVisible(false);
        standingsTable.setManaged(false);
        reportArea.setVisible(true);
        reportArea.setManaged(true);
        knockoutBracket.setVisible(false);
        knockoutBracket.setManaged(false);
        matchdayResultsArea.setVisible(false);
        matchdayResultsArea.setManaged(false);
    }

    private void showKnockoutBracket(boolean visible) {
        standingsTable.setVisible(false);
        standingsTable.setManaged(false);
        reportArea.setVisible(false);
        reportArea.setManaged(false);
        knockoutBracket.setVisible(visible);
        knockoutBracket.setManaged(visible);
        matchdayResultsArea.setVisible(false);
        matchdayResultsArea.setManaged(false);
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

        List<FirstLegMatch> firstLegs = championship.getMatches().stream()
                .map(Match::asFirstLeg)
                .filter(Objects::nonNull)
                .toList();
        List<SecondLegMatch> secondLegs = championship.getMatches().stream()
                .map(Match::asSecondLeg)
                .filter(Objects::nonNull)
                .toList();

        addSeriesCards(quarterColumn, firstLegs, secondLegs, 0, 4);
        addSeriesCards(semiColumn, firstLegs, secondLegs, 4, 2);
        addConnectors(quarterConnectorColumn, 4);
        addConnectors(semiConnectorColumn, 2);

        championship.getMatches().stream()
                .map(Match::asFinal)
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(finalMatch -> {
                    if (finalMatch.isPlayed()
                            && !championship.isPendingPenaltyShootout(finalMatch)) {
                        Team winner = championship.getRecordedFinalWinner(finalMatch);
                        finalColumn.getChildren().add(createMatchCard(List.of(finalMatch), winner));
                        winnerBanner.setText("GANADOR: " + winner.getName());
                    } else if (championship.isPendingPenaltyShootout(finalMatch)) {
                        finalColumn.getChildren().add(createPendingPenaltyCard(finalMatch));
                    } else {
                        finalColumn.getChildren().add(createPendingMatchCard(finalMatch));
                    }
                });
    }

    private void addConnectors(VBox column, int count) {
        for (int i = 0; i < count; i++) {
            Label arrow = new Label("›");
            arrow.getStyleClass().add("bracket-arrow");
            column.getChildren().add(arrow);
        }
    }

    private void addSeriesCards(VBox column, List<FirstLegMatch> firstLegs,
                                List<SecondLegMatch> secondLegs, int startIndex, int count) {
        for (int index = startIndex; index < startIndex + count && index < firstLegs.size(); index++) {
            FirstLegMatch first = firstLegs.get(index);
            SecondLegMatch second = index < secondLegs.size() ? secondLegs.get(index) : null;
            if (second != null && first.isPlayed() && second.isPlayed()
                    && !championship.isPendingPenaltyShootout(second)) {
                Team winner = championship.getRecordedSeriesWinner(first, second);
                column.getChildren().add(createMatchCard(List.of(first, second), winner));
            } else {
                column.getChildren().add(createPendingSeriesCard(first, second));
            }
        }
    }

    @FXML
    private void handleVenuesCrud() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Venues.fxml"));
            Parent view = loader.load();
            VenuesController controller = loader.getController();
            controller.setTournament(championship, tournamentName);
            tournamentPane.getChildren().setAll(view);
        } catch (IOException | IllegalStateException exception) {
            showError("The cities and stadiums screen could not be opened: "
                    + exception.getMessage());
        }
    }

    private VBox createPendingSeriesCard(FirstLegMatch first, SecondLegMatch second) {
        VBox card = new VBox(3);
        card.getStyleClass().add("match-card");
        card.getChildren().add(new Label(first.getHomeTeam().getName()
                + " vs " + first.getAwayTeam().getName()));
        if (first.isPlayed()) {
            card.getChildren().add(new Label("Ida: " + first.getHomeGoals()
                    + " - " + first.getAwayGoals()));
        } else {
            card.getChildren().add(new Label("Ida pendiente"));
        }
        if (second == null || !second.isPlayed()) {
            card.getChildren().add(new Label("Vuelta pendiente"));
        } else if (championship.isPendingPenaltyShootout(second)) {
            card.getChildren().add(new Label("Penales pendientes"));
        }
        if (first.isPlayed()) {
            card.getChildren().add(new Label("Click para ver incidencias"));
            card.setOnMouseClicked(event -> showMatchDetails(
                    second != null && second.isPlayed()
                            ? List.of(first, second)
                            : List.of(first)
            ));
        }
        return card;
    }

    private VBox createPendingPenaltyCard(Match match) {
        VBox card = new VBox(3);
        card.getStyleClass().add("match-card");
        card.getChildren().add(new Label(match.getHomeTeam().getName()
                + " " + match.getHomeGoals() + " - " + match.getAwayGoals()
                + " " + match.getAwayTeam().getName()));
        card.getChildren().add(new Label("Penales pendientes"));
        card.getChildren().add(new Label("Click para ver incidencias"));
        card.setOnMouseClicked(event -> showMatchDetails(List.of(match)));
        return card;
    }

    private VBox createPendingMatchCard(FinalMatch match) {
        VBox card = new VBox(3);
        card.getStyleClass().add("match-card");
        card.getChildren().add(new Label(match.getHomeTeam().getName()
                + " vs " + match.getAwayTeam().getName()));
        card.getChildren().add(new Label("Final pendiente"));
        return card;
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
        card.getChildren().add(new Label("Click para ver incidencias"));
        card.setOnMouseClicked(event -> showMatchDetails(matches));
        return card;
    }

    private void showMatchDetails(List<Match> matches) {
        if (matches.isEmpty()) {
            showInformation("Match details", "Play a match to view its details.");
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
        alert.showAndWait();
    }

    @FXML
    private void handleMatchHistory() {
        if (championship == null) return;
        showMatchDetails(championship.getPlayedMatches().stream()
                .sorted(Comparator.comparing(Match::getMatchDate)).toList());
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
        if (match.isSettledByPenalties()) {
            return true;
        }

        if (match.getHomeGoals() != match.getAwayGoals()) {
            return false;
        }

        return match.getIncidents().stream()
                .anyMatch(Incident::isShootoutPenalty);
    }

    private int[] getPenaltyScore(Match match) {
        int home = 0;
        int away = 0;

        for (Incident incident : match.getIncidents()) {
            if (!incident.isShootoutPenalty()) {
                continue;
            }
            if (!incident.isPenaltyScored()) {
                continue;
            }
            Player penaltyTaker = incident.getPenaltyTaker();
            if (match.getHomeTeam().getPlayers().contains(penaltyTaker)) {
                home++;
            } else if (match.getAwayTeam().getPlayers().contains(penaltyTaker)) {
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

    private void scheduleQuarterFinalsIfNeeded() {
        if (championship.getStage() != TournamentStage.QUARTER_FINALS) {
            return;
        }

        LocalDate lastGroupDate = championship.getMatches().stream()
                .filter(Match::isGroupStage)
                .map(Match::getMatchDate)
                .max(LocalDate::compareTo)
                .orElseThrow();

        championship.scheduleQuarterFinals(lastGroupDate.plusDays(7));
    }
}
