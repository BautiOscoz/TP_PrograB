package Core.Run;

import Core.domain.Match;
import Core.domain.Team;
import Core.domain.TeamStanding;
import Core.domain.TournamentZone;

import java.util.Comparator;
import java.util.List;

public class StandingsTieBreakerTest {
    public static void main(String[] args) throws Exception {
        verifyHeadToHeadBreaksCompleteTie();
        verifyContinentalRankingIsFinalFallback();
        System.out.println("All standings tie-breaker checks passed.");
    }

    private static void verifyHeadToHeadBreaksCompleteTie() throws Exception {
        Championship championship = new Championship("torneo.json", 2026L);
        TournamentZone zone = championship.getTournamentZones().get(0);
        Team teamA = zone.getTeams().get(0);
        Team teamB = zone.getTeams().get(1);
        Team teamC = zone.getTeams().get(2);
        Team teamD = zone.getTeams().get(3);

        setScore(championship, teamA, 1, teamB, 0);
        setScore(championship, teamA, 0, teamC, 1);
        setScore(championship, teamA, 0, teamD, 0);
        setScore(championship, teamB, 1, teamC, 0);
        setScore(championship, teamB, 0, teamD, 0);
        setScore(championship, teamC, 1, teamD, 0);

        List<TeamStanding> standings = championship.getStandings(zone);
        TeamStanding standingA = findStanding(standings, teamA);
        TeamStanding standingB = findStanding(standings, teamB);
        require(standingA.getPoints() == standingB.getPoints(),
                "The test teams must be tied on points.");
        require(standingA.getGoalDifference() == standingB.getGoalDifference(),
                "The test teams must be tied on goal difference.");
        require(standingA.getGoalsFor() == standingB.getGoalsFor(),
                "The test teams must be tied on goals scored.");
        require(standings.indexOf(standingA) < standings.indexOf(standingB),
                "The winner of the head-to-head match must rank first.");
    }

    private static void verifyContinentalRankingIsFinalFallback() throws Exception {
        Championship championship = new Championship("torneo.json", 2027L);
        TournamentZone zone = championship.getTournamentZones().get(0);
        for (Match match : championship.getMatches()) {
            if (match.isGroupStage()
                    && zone.getTeams().contains(match.getHomeTeam())
                    && zone.getTeams().contains(match.getAwayTeam())) {
                match.setHomeGoals(0);
                match.setAwayGoals(0);
                match.setPlayed(true);
            }
        }

        List<TeamStanding> standings = championship.getStandings(zone);
        List<Team> expectedOrder = zone.getTeams().stream()
                .sorted(Comparator.comparingInt(Team::getRanking))
                .toList();
        List<Team> actualOrder = standings.stream()
                .map(TeamStanding::getTeam)
                .toList();
        require(actualOrder.equals(expectedOrder),
                "Continental ranking must provide a deterministic final fallback.");
    }

    private static void setScore(
            Championship championship,
            Team firstTeam,
            int firstGoals,
            Team secondTeam,
            int secondGoals
    ) {
        Match match = championship.getMatches().stream()
                .filter(Match::isGroupStage)
                .filter(candidate ->
                        candidate.getHomeTeam() == firstTeam
                                && candidate.getAwayTeam() == secondTeam
                                || candidate.getHomeTeam() == secondTeam
                                && candidate.getAwayTeam() == firstTeam)
                .findFirst()
                .orElseThrow();

        if (match.getHomeTeam() == firstTeam) {
            match.setHomeGoals(firstGoals);
            match.setAwayGoals(secondGoals);
        } else {
            match.setHomeGoals(secondGoals);
            match.setAwayGoals(firstGoals);
        }
        match.setPlayed(true);
    }

    private static TeamStanding findStanding(List<TeamStanding> standings, Team team) {
        return standings.stream()
                .filter(standing -> standing.getTeam() == team)
                .findFirst()
                .orElseThrow();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
