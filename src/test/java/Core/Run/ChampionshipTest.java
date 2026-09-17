package Core.Run;

import Core.domain.Match;
import Core.domain.Player;
import Core.domain.Team;
import Core.domain.TournamentZone;
import Core.enums.Position;
import Core.incidents.Goal;
import Core.incidents.Substitution;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChampionshipTest {
    public static void main(String[] args) throws Exception {
        Championship championship = new Championship("torneo.json", 2026L);

        require(!championship.getCountries().isEmpty(), "The JSON must create the country catalog.");
        for (Team team : championship.getTeams()) {
            require(championship.getCountries().stream().anyMatch(country -> country == team.getCountry()),
                    "Every team must reference a country from the championship catalog.");
            require(championship.getCountries().stream().anyMatch(country -> country == team.getCoach().getNationality()),
                    "Every coach must reference a country from the championship catalog.");
        }
        championship.getReferees().forEach(referee ->
                require(championship.getCountries().stream().anyMatch(country -> country == referee.getNationality()),
                        "Every referee must reference a country from the championship catalog."));

        require(championship.getTournamentZones().size() == 4, "There must be four zones.");
        for (TournamentZone zone : championship.getTournamentZones()) {
            require(zone.getTeams().size() == 4, "Each zone must contain four teams.");
        }
        verifyBalancedPots(championship);
        require(championship.getMatches().size() == 24, "The group stage must contain 24 matches.");
        for (Match match : championship.getMatches()) {
            require(Championship.isRefereeEligible(match.getReferee(), match.getHomeTeam(), match.getAwayTeam()),
                    "An assigned referee is not eligible.");
        }

        require(
                championship.getCurrentMatchday() == 1,
                "The tournament must start on matchday 1."
        );

        Team teamWithSuspendedPlayer = championship.getTeams().get(0);
        Player suspendedPlayer = teamWithSuspendedPlayer.getPlayers().stream()
                .filter(player -> player.getPosition() == Position.MIDFIELDER)
                .findFirst()
                .orElseThrow();
        suspendedPlayer.setSuspended(true);

        championship.simulateNextMatchday();

        Match suspendedTeamMatch = championship.getMatches().stream()
                .filter(Match::isPlayed)
                .filter(match -> match.getHomeTeam() == teamWithSuspendedPlayer
                        || match.getAwayTeam() == teamWithSuspendedPlayer)
                .findFirst()
                .orElseThrow();
        var suspendedTeamLineup = suspendedTeamMatch.getHomeTeam() == teamWithSuspendedPlayer
                ? suspendedTeamMatch.getHomeLineup()
                : suspendedTeamMatch.getAwayLineup();
        require(!suspendedTeamLineup.getPlayers().contains(suspendedPlayer),
                "A suspended player cannot be in the initial lineup.");
        require(suspendedTeamMatch.getIncidents().stream()
                        .filter(Substitution.class::isInstance)
                        .map(Substitution.class::cast)
                        .noneMatch(substitution -> substitution.getPlayerIn() == suspendedPlayer),
                "A suspended player cannot enter as a substitute.");
        require(!suspendedPlayer.isSuspended(),
                "The suspension must be served after missing one match.");

        long playedAfterFirstMatchday =
                championship.getMatches().stream()
                        .filter(Match::isPlayed)
                        .count();

        require(
                playedAfterFirstMatchday == 8,
                "Exactly eight matches must be played per matchday."
        );

        require(
                championship.getCurrentMatchday() == 2,
                "Matchday 2 must follow matchday 1."
        );


        championship.simulateGroupStage();


        for (Match match : championship.getMatches()) {
            require(match.isPlayed(), "Every group match must be played.");
            require(match.getHomeLineup().getPlayers().size() == 11
                            && match.getAwayLineup().getPlayers().size() == 11,
                    "Every match must have two initial lineups of 11 players.");
            long recordedGoals = match.getIncidents().stream().filter(Goal.class::isInstance).count();
            require(recordedGoals == match.getHomeGoals() + match.getAwayGoals(),
                    "Every goal in the score must be recorded as an incident.");
            verifyChronologicalIncidents(match);
            verifyGoalScorersWereOnField(match);
        }
        System.out.println("All championship checks passed.");
    }

    private static void verifyChronologicalIncidents(Match match) {
        int previousMinute = -1;
        for (var incident : match.getIncidents()) {
            require(incident.getMinute() >= previousMinute,
                    "Match incidents must be ordered chronologically.");
            previousMinute = incident.getMinute();
        }
    }

    private static void verifyGoalScorersWereOnField(Match match) {
        for (var incident : match.getIncidents()) {
            if (!(incident instanceof Goal goal)) continue;

            Player scorer = goal.getScorer();
            Team scorerTeam = match.getHomeTeam().getPlayers().contains(scorer)
                    ? match.getHomeTeam()
                    : match.getAwayTeam();
            require(match.getPlayersOnField(scorerTeam, goal.getMinute()).contains(scorer),
                    "A goal scorer must be on the field at the goal minute.");
        }
    }

    private static void verifyBalancedPots(Championship championship) {
        List<Team> ranked = new ArrayList<>(championship.getTeams());
        ranked.sort(Comparator.comparingInt(Team::getRanking));
        for (TournamentZone zone : championship.getTournamentZones()) {
            for (int pot = 0; pot < 4; pot++) {
                List<Team> potTeams = ranked.subList(pot * 4, pot * 4 + 4);
                long teamsFromPot = zone.getTeams().stream().filter(potTeams::contains).count();
                require(teamsFromPot == 1, "Each zone must receive one team from every ranking pot.");
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
