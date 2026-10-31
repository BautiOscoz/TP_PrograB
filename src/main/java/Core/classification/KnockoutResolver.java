package Core.classification;
import Core.domain.*;
import Core.incidents.Incident;

/** Read-only knockout rules. Querying a result never simulates penalties. */
public final class KnockoutResolver {
    /** Empty means that the series requires a penalty shootout. */
    public java.util.Optional<Team> seriesWinnerBeforePenalties(FirstLegMatch first, SecondLegMatch second) {
        Team home = first.getHomeTeam(), away = first.getAwayTeam();
        int homePoints = getSeriesPoints(first, second, home);
        int awayPoints = getSeriesPoints(first, second, away);
        if (homePoints != awayPoints) return java.util.Optional.of(homePoints > awayPoints ? home : away);
        int homeDifference = getWeightedGoalDifference(first, second, home);
        int awayDifference = getWeightedGoalDifference(first, second, away);
        return homeDifference == awayDifference ? java.util.Optional.empty()
                : java.util.Optional.of(homeDifference > awayDifference ? home : away);
    }
    public int getMatchPoints(
            int goalsFor,
            int goalsAgainst
    ) {
        if (goalsFor > goalsAgainst) {
            return 3;
        }

        if (goalsFor == goalsAgainst) {
            return 1;
        }

        return 0;
    }

    public int getSeriesPoints(
            FirstLegMatch firstLeg,
            SecondLegMatch secondLeg,
            Team team
    ) {
        if (team.equals(firstLeg.getHomeTeam())) {

            int firstLegPoints = getMatchPoints(
                    firstLeg.getHomeGoals(),
                    firstLeg.getAwayGoals()
            );

            int secondLegPoints = getMatchPoints(
                    secondLeg.getAwayGoals(),
                    secondLeg.getHomeGoals()
            );

            return firstLegPoints + secondLegPoints;
        }

        if (team.equals(firstLeg.getAwayTeam())) {

            int firstLegPoints = getMatchPoints(
                    firstLeg.getAwayGoals(),
                    firstLeg.getHomeGoals()
            );

            int secondLegPoints = getMatchPoints(
                    secondLeg.getHomeGoals(),
                    secondLeg.getAwayGoals()
            );

            return firstLegPoints + secondLegPoints;
        }

        throw new IllegalArgumentException(
                "Team does not belong to this series."
        );
    }

    public int getWeightedGoalDifference(
            FirstLegMatch firstLeg,
            SecondLegMatch secondLeg,
            Team team
    ) {
        Team teamA = firstLeg.getHomeTeam();
        Team teamB = firstLeg.getAwayTeam();

        if (team.equals(teamA)) {

            int goalsFor =
                    firstLeg.getHomeGoals()
                            + secondLeg.getAwayGoals() * 2;

            int goalsAgainst =
                    firstLeg.getAwayGoals() * 2
                            + secondLeg.getHomeGoals();

            return goalsFor - goalsAgainst;
        }

        if (team.equals(teamB)) {

            int goalsFor =
                    firstLeg.getAwayGoals() * 2
                            + secondLeg.getHomeGoals();

            int goalsAgainst =
                    firstLeg.getHomeGoals()
                            + secondLeg.getAwayGoals() * 2;

            return goalsFor - goalsAgainst;
        }

        throw new IllegalArgumentException(
                "Team does not belong to this series."
        );
    }

    public Team getRecordedSeriesWinner(FirstLegMatch firstLeg, SecondLegMatch secondLeg) {
        if (!firstLeg.isPlayed() || !secondLeg.isPlayed()) {
            throw new IllegalStateException("Both legs must be played.");
        }
        Team teamA = firstLeg.getHomeTeam();
        Team teamB = firstLeg.getAwayTeam();
        int pointsA = getSeriesPoints(firstLeg, secondLeg, teamA);
        int pointsB = getSeriesPoints(firstLeg, secondLeg, teamB);
        if (pointsA != pointsB) {
            return pointsA > pointsB ? teamA : teamB;
        }
        int differenceA = getWeightedGoalDifference(firstLeg, secondLeg, teamA);
        int differenceB = getWeightedGoalDifference(firstLeg, secondLeg, teamB);
        if (differenceA != differenceB) {
            return differenceA > differenceB ? teamA : teamB;
        }
        return recordedPenaltyWinner(secondLeg);
    }

    public boolean hasRecordedShootout(Match match) {
        return match.getIncidents().stream().anyMatch(Incident::isShootoutPenalty);
    }

    public Team recordedPenaltyWinner(Match match) {
        int home = 0;
        int away = 0;
        for (Incident incident : match.getIncidents()) {
            if (!incident.isShootoutPenalty() || !incident.isPenaltyScored()) {
                continue;
            }
            Player penaltyTaker = incident.getPenaltyTaker();
            if (match.getHomeTeam().getPlayers().contains(penaltyTaker)) {
                home++;
            } else if (match.getAwayTeam().getPlayers().contains(penaltyTaker)) {
                away++;
            }
        }
        if (!hasRecordedShootout(match) || home == away) {
            throw new IllegalStateException("No decided shootout was recorded.");
        }
        return home > away ? match.getHomeTeam() : match.getAwayTeam();
    }

    public Team getRecordedFinalWinner(FinalMatch finalMatch) {
        if (!finalMatch.isPlayed()) {
            throw new IllegalStateException("The final has not been played.");
        }
        if (finalMatch.getHomeGoals() != finalMatch.getAwayGoals()) {
            return finalMatch.getHomeGoals() > finalMatch.getAwayGoals()
                    ? finalMatch.getHomeTeam() : finalMatch.getAwayTeam();
        }
        return recordedPenaltyWinner(finalMatch);
    }
}
