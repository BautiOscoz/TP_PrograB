package GUI;

import Core.Run.Championship;
import Core.domain.Player;
import Core.domain.Team;
import Core.report.TournamentReportService;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.VBox;



import Core.report.TournamentReportService.TeamStatsRow;
import Core.report.TournamentReportService.RefereeStatsRow;

import static GUI.RankingTableFactory.*;

/** Displays the tournament rankings using the report service. */
public final class RankingsView {
    private final VBox rankingPanel;
    private final RankingPanelView rankingView;
    private final TournamentReportService reportService;

    public RankingsView(VBox rankingPanel, RankingPanelView rankingView, TournamentReportService reportService) {
        this.rankingPanel = rankingPanel;
        this.rankingView = rankingView;
        this.reportService = reportService;
    }
    public void showTopScorers(Championship championship) {
        rankingView.showRanking("Top Scorers", reportService.getTopScorers(championship),
                rankingColumn("Player", row -> row.getPlayer().getName() + " " + row.getPlayer().getLastName()),
                rankingColumn("Team", row -> row.getTeam().getName()),
                rankingColumn("Goals", TournamentReportService.TopScorerRow::getGoals),
                rankingColumn("Penalty goals", TournamentReportService.TopScorerRow::getPenaltyGoals));
    }

    public void showParticipation(Championship championship) {
        rankingView.showRanking("Participation Ranking", reportService.getParticipation(championship),
                rankingColumn("Player", row -> row.getPlayer().getName() + " " + row.getPlayer().getLastName()),
                rankingColumn("Team", row -> row.getTeam().getName()),
                rankingColumn("Matches", TournamentReportService.ParticipationRow::getMatches),
                rankingColumn("Minutes", TournamentReportService.ParticipationRow::getMinutes));
    }

    public void showFairPlay(Championship championship) {
        rankingView.showRanking("Fair Play", reportService.getFairPlay(championship),
                rankingColumn("Team", row -> row.getTeam().getName()),
                rankingColumn("Yellow cards", TournamentReportService.FairPlayRow::getYellow),
                rankingColumn("Red cards", TournamentReportService.FairPlayRow::getRed),
                rankingColumn("Points", TournamentReportService.FairPlayRow::getPoints));
    }

    public void showTeamsStats(Championship championship) {
        TableColumn<TeamStatsRow, Integer> goalsFor =
                rankingColumn("GF", TeamStatsRow::getGoalsFor);

        TableColumn<TeamStatsRow, Integer> goalsAgainst =
                rankingColumn("GA", TeamStatsRow::getGoalsAgainst);

        // Las columnas de goles necesitan menos espacio.
        goalsFor.setMinWidth(45);
        goalsFor.setPrefWidth(60);

        goalsAgainst.setMinWidth(45);
        goalsAgainst.setPrefWidth(60);

        rankingView.showRanking(
                "Teams Stats",
                reportService.getTeamStats(championship),

                rankingColumn("Team",
                        row -> row.getTeam().getName()),

                decimalRankingColumn("Avg age",
                        TeamStatsRow::getAverageAge, "%.1f"),

                rankingColumn("Coach",
                        TeamStatsRow::getCoach),

                rankingColumn("Coach age",
                        TeamStatsRow::getCoachAge),

                rankingColumn("Coach nationality",
                        TeamStatsRow::getCoachNationality),

                goalsFor,
                goalsAgainst,

                decimalRankingColumn("Effectiveness",
                        TeamStatsRow::getEffectiveness, "%.1f%%")
        );
    }

    public void showRefereesStats(Championship championship) {
        rankingView.showRanking(
                "Referees Stats",
                reportService.getRefereeStats(championship),

                rankingColumn("Referee",
                        row -> row.getReferee().getName()
                                + " " + row.getReferee().getLastName()),

                rankingColumn("Matches",
                        RefereeStatsRow::getMatches),

                rankingColumn("Years of experience",
                        RefereeStatsRow::getYears)
        );

        Label average = new Label(String.format(
                "Average years of experience: %.2f years",
                reportService.getAverageRefereeExperience(championship)
        ));

        average.getStyleClass().add("ranking-summary");
        rankingPanel.getChildren().add(average);
    }
}
