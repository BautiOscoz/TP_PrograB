package Core.report;

import Core.classification.StandingsCalculator;
import Core.domain.Match;
import Core.domain.Team;
import Core.domain.TeamStanding;
import Core.domain.TournamentZone;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GroupEvolutionReportService {

    private final StandingsCalculator calculator =
            new StandingsCalculator();

    public String buildReport(
            TournamentZone zone,
            List<Match> matches
    ) {
        List<Match> zoneMatches = new ArrayList<>();

        for (Match match : matches) {
            if (match.isGroupStage()
                    && match.isPlayed()
                    && zone.getTeams().contains(match.getHomeTeam())
                    && zone.getTeams().contains(match.getAwayTeam())) {

                zoneMatches.add(match);
            }
        }

        // Ordena por fecha del torneo y conserva el orden
        // original de los partidos de una misma fecha.
        zoneMatches.sort(
                Comparator.comparingInt(Match::getMatchday)
        );

        StringBuilder text = new StringBuilder();

        text.append("STANDINGS EVOLUTION - ")
                .append(zone.getName())
                .append("\n\n");

        if (zoneMatches.isEmpty()) {
            text.append("No group matches have been played yet.\n");
        } else {
            List<Match> previousMatches = new ArrayList<>();
            int currentMatchday = 0;

            for (Match match : zoneMatches) {
                if (match.getMatchday() != currentMatchday) {
                    currentMatchday = match.getMatchday();

                    text.append("MATCHDAY ")
                            .append(currentMatchday)
                            .append("\n\n");
                }

                text.append(match.getHomeTeam().getName())
                        .append(" vs ")
                        .append(match.getAwayTeam().getName())
                        .append("\n\n");

                // Todavía no incorporamos el partido actual.
                text.append("BEFORE THE MATCH\n");

                appendTable(
                        text,
                        zone,
                        calculator.calculate(zone, previousMatches)
                );

                text.append("\nRESULT: ")
                        .append(match.getHomeTeam().getName())
                        .append(" ")
                        .append(match.getHomeGoals())
                        .append(" - ")
                        .append(match.getAwayGoals())
                        .append(" ")
                        .append(match.getAwayTeam().getName())
                        .append("\n\n");

                // Ahora incorporamos su resultado.
                previousMatches.add(match);

                text.append("AFTER THE MATCH\n");

                appendTable(
                        text,
                        zone,
                        calculator.calculate(zone, previousMatches)
                );

                text.append(
                        "\n------------------------------------------------------------\n\n"
                );
            }
        }

        return text.toString();
    }

    private void appendTable(
            StringBuilder text,
            TournamentZone zone,
            List<TeamStanding> standings
    ) {
        int teamWidth = 20;

        for (Team team : zone.getTeams()) {
            teamWidth = Math.max(
                    teamWidth,
                    team.getName().length()
            );
        }

        String format = "%-4s %-" + teamWidth
                + "s %4s %3s %3s %3s %3s %4s %4s %4s%n";

        text.append(
                String.format(
                        Locale.US,
                        format,
                        "POS",
                        "TEAM",
                        "PTS",
                        "P",
                        "W",
                        "D",
                        "L",
                        "GF",
                        "GA",
                        "GD"
                )
        );

        int position = 1;

        for (TeamStanding standing : standings) {
            text.append(
                    String.format(
                            Locale.US,
                            format,
                            position,
                            standing.getTeam().getName(),
                            standing.getPoints(),
                            standing.getPlayed(),
                            standing.getWon(),
                            standing.getDrawn(),
                            standing.getLost(),
                            standing.getGoalsFor(),
                            standing.getGoalsAgainst(),
                            standing.getGoalDifference()
                    )
            );

            position++;
        }
    }
}
