package GUI;

import Core.Run.Championship;
import Core.domain.TeamStanding;
import Core.domain.TournamentZone;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;


import java.util.List;

import Core.report.GroupEvolutionReportService;


/** Displays standings and match-by-match group evolution. */
public final class GroupStageView {
    private final Label zoneTitleLabel;
    private final Label phaseLabel;
    private final TableView<TeamStanding> standingsTable;
    private final TableColumn<TeamStanding, Number> positionColumn;
    private final TableColumn<TeamStanding, String> teamColumn;
    private final TableColumn<TeamStanding, Number> pointsColumn;
    private final TableColumn<TeamStanding, Number> playedColumn;
    private final TableColumn<TeamStanding, Number> wonColumn;
    private final TableColumn<TeamStanding, Number> drawnColumn;
    private final TableColumn<TeamStanding, Number> lostColumn;
    private final TableColumn<TeamStanding, Number> goalsForColumn;
    private final TableColumn<TeamStanding, Number> goalsAgainstColumn;
    private final TableColumn<TeamStanding, Number> differenceColumn;
    private final TextArea matchdayResultsArea;

    public GroupStageView(Label zoneTitleLabel,
            Label phaseLabel,
            TableView<TeamStanding> standingsTable,
            TableColumn<TeamStanding,
            Number> positionColumn,
            TableColumn<TeamStanding,
            String> teamColumn,
            TableColumn<TeamStanding,
            Number> pointsColumn,
            TableColumn<TeamStanding,
            Number> playedColumn,
            TableColumn<TeamStanding,
            Number> wonColumn,
            TableColumn<TeamStanding,
            Number> drawnColumn,
            TableColumn<TeamStanding,
            Number> lostColumn,
            TableColumn<TeamStanding,
            Number> goalsForColumn,
            TableColumn<TeamStanding,
            Number> goalsAgainstColumn,
            TableColumn<TeamStanding,
            Number> differenceColumn,
            TextArea matchdayResultsArea) {
        this.zoneTitleLabel = zoneTitleLabel;
        this.phaseLabel = phaseLabel;
        this.standingsTable = standingsTable;
        this.positionColumn = positionColumn;
        this.teamColumn = teamColumn;
        this.pointsColumn = pointsColumn;
        this.playedColumn = playedColumn;
        this.wonColumn = wonColumn;
        this.drawnColumn = drawnColumn;
        this.lostColumn = lostColumn;
        this.goalsForColumn = goalsForColumn;
        this.goalsAgainstColumn = goalsAgainstColumn;
        this.differenceColumn = differenceColumn;
        this.matchdayResultsArea = matchdayResultsArea;
        configureColumns();
    }
    private final GroupEvolutionReportService groupEvolutionReportService = new GroupEvolutionReportService();
    public void showZone(Championship championship, int zoneIndex) {
        if (championship != null) {
            List<TournamentZone> zones =
                    championship.getTournamentZones();

            if (zoneIndex >= 0 && zoneIndex < zones.size()) {
                TournamentZone zone = zones.get(zoneIndex);


                phaseLabel.setText("Group Stage");
                zoneTitleLabel.setVisible(true);
                zoneTitleLabel.setText(zone.getName());

                // Current group standings.
                standingsTable.getItems().setAll(
                        championship.getStandings(zone)
                );

                // Standings evolution after each match.
                matchdayResultsArea.setText(
                        groupEvolutionReportService.buildReport(
                                zone,
                                championship.getMatches()
                        )
                );

                matchdayResultsArea.positionCaret(0);
            }
        }
    }
    private void configureColumns() {
        standingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        standingsTable.getColumns().forEach(column -> {
            column.setSortable(false);
            column.setReorderable(false);
            column.setResizable(false);
            column.setMinWidth(column == teamColumn ? 150 : 42);
        });
        teamColumn.getStyleClass().add("team-column");
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
}
