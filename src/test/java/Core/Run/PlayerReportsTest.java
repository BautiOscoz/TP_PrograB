package Core.Run;

import Core.domain.Match;
import Core.domain.Player;
import Core.enums.Position;
import Core.incidents.Incident;
import Core.report.TournamentReportService;

/** Run directly: the project currently uses executable test classes. */
public class PlayerReportsTest {
    public static void main(String[] args) throws Exception {
        Championship tournament = new Championship("torneo.json", 2026L);
        TournamentReportService reports = new TournamentReportService();
        require(reports.getPlayerStats(tournament).stream()
                .allMatch(row -> row.getMatches() == 0 && row.getGoals() == 0), "New tournament");
        tournament.simulateGroupStage();

        for (var row : reports.getPlayerStats(tournament)) {
            Player player = row.getPlayer();
            int goals = 0, conceded = 0, matches = 0, minutes = 0;
            for (Match match : tournament.getPlayedMatches()) {
                for (Incident incident : match.getIncidents()) {
                    if (incident.getScoringPlayer() == player) goals++;
                    if (incident.isMatchGoal() && incident.getGoalkeeper() == player) conceded++;
                }
                boolean starter = match.getHomeLineup().getPlayers().contains(player)
                        || match.getAwayLineup().getPlayers().contains(player);
                int entry = starter ? 0 : match.getIncidents().stream()
                        .filter(i -> i.getPlayerIn() == player).mapToInt(Incident::getMinute)
                        .min().orElse(-1);
                if (entry >= 0) {
                    matches++;
                    int exit = match.getIncidents().stream()
                            .filter(i -> i.getPlayerOut() == player
                                    || (i.isRedCard() && i.getAffectedPlayer() == player))
                            .mapToInt(Incident::getMinute).min().orElse(90);
                    minutes += Math.max(0, exit - entry);
                }
            }
            require(row.getGoals() == goals && row.getGoalsAgainst() == conceded, "Goal counts");
            require(row.getMatches() == matches && row.getMinutes() == minutes, "Participation");
        }
        for (Position position : Position.values()) {
            var rows = reports.getPlayerStats(tournament, position);
            long expected = tournament.getTeams().stream().flatMap(t -> t.getPlayers().stream())
                    .filter(p -> p.getPosition() == position).count();
            require(rows.size() == expected && rows.stream()
                    .allMatch(r -> r.getPlayer().getPosition() == position), "Position filter");
        }
        double expectedAverage = tournament.getReferees().stream()
                .mapToInt(r -> r.getRefereeYears()).average().orElse(0);
        require(reports.getAverageRefereeExperience(tournament) == expectedAverage, "Referee average");
        System.out.println("All player report checks passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
