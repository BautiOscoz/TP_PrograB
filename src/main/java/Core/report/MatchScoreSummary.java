package Core.report;

import Core.domain.Match;
import Core.domain.Player;
import Core.incidents.Incident;





/** Reads aggregate and penalty scores for presentation. */
public final class MatchScoreSummary {
    private MatchScoreSummary() {}
    public static int[] getAggregateScore(Match firstLeg, Match secondLeg) {
        int homeAggregate = firstLeg.getHomeGoals() + secondLeg.getAwayGoals();
        int awayAggregate = firstLeg.getAwayGoals() + secondLeg.getHomeGoals();
        return new int[]{homeAggregate, awayAggregate};
    }

    public static boolean isSettledByPenalties(Match match) {
        if (match.isSettledByPenalties()) {
            return true;
        }

        if (match.getHomeGoals() != match.getAwayGoals()) {
            return false;
        }

        return match.getIncidents().stream()
                .anyMatch(Incident::isShootoutPenalty);
    }

    public static int[] getPenaltyScore(Match match) {
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
}
