package Core.report;

import Core.Run.Championship;
import Core.domain.*;
import Core.incidents.Incident;
import java.util.Comparator;
import java.util.List;

/** Read-only presentation of recorded matches; never runs the simulation. */
public class MatchDetailsFormatter {
    public String title(Match match) {
        String stage = match.isGroupStage() ? "Group matchday " + match.getMatchday()
                : match.asFirstLeg() != null ? "First leg"
                : match.asSecondLeg() != null ? "Second leg" : "Final";
        return match.getMatchDate() + " | " + stage + " | "
                + match.getHomeTeam().getName() + " " + match.getHomeGoals()
                + " - " + match.getAwayGoals() + " " + match.getAwayTeam().getName();
    }

    public String format(Championship championship, Match match) {
        StringBuilder text = new StringBuilder(title(match)).append("\n\n");
        if (!match.isPlayed()) return text.append("Not played yet.").toString();
        text.append("Referee: ").append(name(match.getReferee())).append("\nVenue: ");
        Stadium stadium = match.getStadium();
        text.append(stadium == null ? "Not assigned" : stadium.getName()
                + " - " + stadium.getCity().getName()).append("\n");
        if (match.isGroupStage()) {
            text.append(match.getHomeGoals() == match.getAwayGoals() ? "Draw: one point each."
                    : "Winner: " + (match.getHomeGoals() > match.getAwayGoals()
                    ? match.getHomeTeam() : match.getAwayTeam()).getName() + " (three points).");
        } else {
            text.append(championship.getKnockoutDecisionCriterion(match));
            if (!championship.isPendingPenaltyShootout(match)) {
                SecondLegMatch second = match.asSecondLeg();
                if (second != null) {
                    FirstLegMatch first = championship.getMatches().stream().map(Match::asFirstLeg)
                            .filter(java.util.Objects::nonNull)
                            .filter(m -> m.getHomeTeam() == second.getAwayTeam()
                                    && m.getAwayTeam() == second.getHomeTeam()).findFirst().orElseThrow();
                    text.append("\nSeries winner: ")
                            .append(championship.getRecordedSeriesWinner(first, second).getName());
                } else if (match.asFinal() != null) {
                    text.append("\nChampion: ")
                            .append(championship.getRecordedFinalWinner(match.asFinal()).getName());
                }
            }
        }
        lineup(text, match.getHomeTeam(), match.getHomeLineup());
        lineup(text, match.getAwayTeam(), match.getAwayLineup());
        text.append("\nMATCH INCIDENTS (90 minutes)\n");
        List<Incident> incidents = match.getIncidents().stream()
                .filter(i -> !i.isShootoutPenalty())
                .sorted(Comparator.comparingInt(Incident::getMinute)).toList();
        if (incidents.isEmpty()) text.append("No incidents recorded.\n");
        for (Incident incident : incidents) {
            text.append(incident.getMinute()).append("' ").append(incident.getDescription());
            if (incident.isMatchGoal()) {
                text.append(" | Goalkeeper: ").append(name(incident.getGoalkeeper()));
            }
            text.append("\n");
        }
        List<Incident> penalties = match.getIncidents().stream()
                .filter(Incident::isShootoutPenalty).toList();
        if (championship.isPendingPenaltyShootout(match)) {
            text.append("\nPENALTY SHOOTOUT: pending. Use Simulate Penalties to continue.\n");
        } else if (!penalties.isEmpty()) {
            int home = 0, away = 0, attempt = 0;
            text.append("\nPENALTY SHOOTOUT (separate from the match score)\n");
            for (Incident penalty : penalties) {
                boolean homeTeam = match.getHomeTeam().getPlayers().contains(penalty.getPenaltyTaker());
                if (penalty.isPenaltyScored()) { if (homeTeam) home++; else away++; }
                text.append(++attempt).append(". ")
                        .append((homeTeam ? match.getHomeTeam() : match.getAwayTeam()).getName())
                        .append(" | ").append(name(penalty.getPenaltyTaker()))
                        .append(penalty.isPenaltyScored() ? " - SCORED" : " - MISSED").append("\n");
            }
            text.append("Shootout result: ").append(home).append(" - ").append(away).append("\n");
        }
        return text.toString();
    }

    private void lineup(StringBuilder text, Team team, Lineup lineup) {
        text.append("\nSTARTING LINEUP - ").append(team.getName()).append("\n");
        if (lineup == null) { text.append("Not recorded.\n"); return; }
        text.append("Formation: ").append(lineup.getFormation()).append("\n");
        for (Player player : lineup.getPlayers()) {
            text.append("#").append(player.getShirtNumber()).append(" ").append(name(player))
                    .append(" - ").append(player.getPosition()).append("\n");
        }
    }

    private String name(Person person) {
        return person == null ? "Not recorded" : person.getName() + " " + person.getLastName();
    }
}
