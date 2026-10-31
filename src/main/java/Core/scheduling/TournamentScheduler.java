package Core.scheduling;
import Core.domain.*;
import java.time.LocalDate;
import java.util.*;

/** Creates fixtures without modifying the championship. */
public final class TournamentScheduler {
    private final Random random;
    private final List<Referee> referees;
    public TournamentScheduler(Random random, List<Referee> referees) {
        this.random = random;
        this.referees = List.copyOf(referees);
    }
    public List<TournamentZone> drawBalancedZones(List<Team> teams) {
        List<Team> rankedTeams = new ArrayList<>(teams);
        rankedTeams.sort(Comparator.comparingInt(Team::getRanking));
        List<TournamentZone> zones = new ArrayList<>();
        for (int i = 0; i < 4; i++) zones.add(new TournamentZone("Zone " + (char) ('A' + i)));

        for (int potIndex = 0; potIndex < 4; potIndex++) {
            List<Team> pot = new ArrayList<>(rankedTeams.subList(potIndex * 4, potIndex * 4 + 4));
            Collections.shuffle(pot, random);
            for (int zoneIndex = 0; zoneIndex < 4; zoneIndex++) {
                zones.get(zoneIndex).addTeam(pot.get(zoneIndex));
            }
        }
        return zones;
    }
    public List<Match> generateGroupMatches(
            List<TournamentZone> tournamentZones, LocalDate startDate
    ) {
        List<Match> generated = new ArrayList<>();

        int[][][] schedule = {
                {
                        {0, 3},
                        {1, 2}
                },
                {
                        {3, 2},
                        {0, 1}
                },
                {
                        {1, 3},
                        {2, 0}
                }
        };

        for (TournamentZone zone : tournamentZones) {
            List<Team> zoneTeams = zone.getTeams();

            for (
                    int matchdayIndex = 0;
                    matchdayIndex < schedule.length;
                    matchdayIndex++
            ) {
                for (int[] pairing : schedule[matchdayIndex]) {
                    Team home =
                            zoneTeams.get(pairing[0]);

                    Team away =
                            zoneTeams.get(pairing[1]);

                    generated.add(
                            new GroupMatch(
                                    startDate.plusDays(matchdayIndex),
                                    home,
                                    away,
                                    chooseEligibleReferee(home, away),
                                    matchdayIndex + 1
                            )
                    );
                }
            }
        }
        return generated;
    }
    private Referee chooseEligibleReferee(Team home, Team away) {
        List<Referee> eligible = referees.stream()
                .filter(referee -> isRefereeEligible(referee, home, away)).toList();
        if (eligible.isEmpty()) {
            throw new IllegalStateException("No eligible referee for " + home.getName() + " vs " + away.getName());
        }
        return eligible.get(random.nextInt(eligible.size()));
    }
    public static boolean isRefereeEligible(Referee referee, Team home, Team away) {
        if (home.getCountry().equals(away.getCountry())) return true;
        return !referee.getNationality().equals(home.getCountry())
                && !referee.getNationality().equals(away.getCountry());
    }
    public List<FirstLegMatch> firstLegs(Team[][] pairings, LocalDate date) {
        List<FirstLegMatch> fixtures = new ArrayList<>();
        for (Team[] pairing : pairings) fixtures.add(new FirstLegMatch(date, pairing[0], pairing[1],
                chooseEligibleReferee(pairing[0], pairing[1])));
        return fixtures;
    }
    public List<FirstLegMatch> quarterFinals(List<List<TeamStanding>> tables, LocalDate date) {
        Team[][] pairings = {
            {tables.get(0).get(0).getTeam(), tables.get(3).get(1).getTeam()},
            {tables.get(1).get(0).getTeam(), tables.get(2).get(1).getTeam()},
            {tables.get(2).get(0).getTeam(), tables.get(0).get(1).getTeam()},
            {tables.get(3).get(0).getTeam(), tables.get(1).get(1).getTeam()}
        };
        return firstLegs(pairings, date);
    }
    public List<SecondLegMatch> secondLegs(List<FirstLegMatch> firstLegs, LocalDate date) {
        List<SecondLegMatch> fixtures = new ArrayList<>();
        for (FirstLegMatch first : firstLegs) {
            Team home = first.getAwayTeam(), away = first.getHomeTeam();
            fixtures.add(new SecondLegMatch(date, home, away, chooseEligibleReferee(home, away),
                    first.getHomeGoals(), first.getAwayGoals()));
        }
        return fixtures;
    }
    public FinalMatch finalMatch(List<Team> finalists, LocalDate date) {
        Team home = finalists.get(0), away = finalists.get(1);
        return new FinalMatch(date, home, away, chooseEligibleReferee(home, away));
    }

}
