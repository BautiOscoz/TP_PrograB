package Core.report;

import Core.domain.Match;
import Core.domain.Player;
import Core.incidents.Incident;

/** Shared presentation for the interface and console, including the participant's team. */
public final class IncidentFormatter {
    private IncidentFormatter() {}

    public static String describe(Match match, Incident incident) {
        Player player = incident.getParticipant();
        String team = player != null && match.getHomeTeam().getPlayers().contains(player)
                ? match.getHomeTeam().getName()
                : player != null && match.getAwayTeam().getPlayers().contains(player)
                ? match.getAwayTeam().getName() : "Unknown team";
        return "[" + team + "] " + incident.getDescription();
    }
}
