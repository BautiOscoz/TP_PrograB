package Core.Run;

import Core.domain.Match;
import Core.domain.Team;
import Core.domain.TournamentZone;
import Core.incidents.Goal;

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

        championship.simulateGroupStage();
        for (Match match : championship.getMatches()) {
            require(match.isPlayed(), "Every group match must be played.");
            require(match.getHomeLineup().getPlayers().size() == 11
                            && match.getAwayLineup().getPlayers().size() == 11,
                    "Every match must have two initial lineups of 11 players.");
            long recordedGoals = match.getIncidents().stream().filter(Goal.class::isInstance).count();
            require(recordedGoals == match.getHomeGoals() + match.getAwayGoals(),
                    "Every goal in the score must be recorded as an incident.");
        }
        System.out.println("All championship checks passed.");
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
