package Core.Run;

import Core.console.MatchConsoleReporter;
import Core.domain.Country;
import Core.domain.GroupMatch;
import Core.domain.Match;
import Core.domain.Referee;
import Core.domain.Team;
import Core.domain.TournamentZone;
import Core.loader.TournamentData;
import Core.loader.TournamentLoader;
import Core.persistance.ChampionshipRepository;
import Core.simulation.MatchSimulator;
import Core.domain.TeamStanding;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

import Core.domain.City;
import Core.domain.Stadium;

import Core.console.MatchConsoleReporter;
import Core.console.StandingsConsoleReporter;

import java.io.Serial;
public class Championship implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private final List<TournamentZone> tournamentZones;
    private final List<Match> matches;
    private final List<Team> teams;
    private final List<Referee> referees;
    private final List<Country> countries;
    private final List<City> cities;
    private final List<Stadium> stadiums;
    private transient Random random;
    private transient MatchSimulator matchSimulator;

    public Championship(String jsonPath) throws IOException {
        this(jsonPath, new Random());
    }

    public Championship(String jsonPath, long seed) throws IOException {
        this(jsonPath, new Random(seed));
    }



    private Championship(String jsonPath, Random random) throws IOException {
        TournamentData tournament = TournamentLoader.load(jsonPath);
        this.teams = new ArrayList<>(tournament.getTeams());
        this.referees = new ArrayList<>(tournament.getReferees());
        this.countries = new ArrayList<>(tournament.getCountries());
        this.cities = new ArrayList<>();
        this.stadiums = new ArrayList<>();
        this.random = random;
        this.matchSimulator = new MatchSimulator(random);
        this.tournamentZones = drawBalancedZones();
        this.matches = generateGroupMatches(LocalDate.now());
    }

    private List<TournamentZone> drawBalancedZones() {
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

    private List<Match> generateGroupMatches(
            LocalDate startDate
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

    public int getCurrentMatchday() {
        return matches.stream()
                .filter(GroupMatch.class::isInstance)
                .map(GroupMatch.class::cast)
                .filter(match -> !match.isPlayed())
                .mapToInt(GroupMatch::getMatchday)
                .min()
                .orElse(0);
    }

    public boolean hasPendingGroupMatchdays() {
        return getCurrentMatchday() != 0;
    }

    public void simulateGroupStage() {
        simulateGroupStage(match -> { });
    }

    public void simulateGroupStage(
            Consumer<Match> afterEachMatch
    ) {
        while (hasPendingGroupMatchdays()) {
            simulateNextMatchday(afterEachMatch);
        }
    }

    public void simulateNextMatchday() {
        simulateNextMatchday(match -> { });
    }

    public void simulateNextMatchday(
            Consumer<Match> afterEachMatch
    ) {
        int matchday = getCurrentMatchday();
        if (matchday == 0) {
            return;
        }
        matches.stream()
                .filter(GroupMatch.class::isInstance)
                .map(GroupMatch.class::cast)
                .filter(match ->
                        match.getMatchday() == matchday
                )
                .filter(match -> !match.isPlayed())
                .forEach(match -> {
                    matchSimulator.simulate(match);
                    afterEachMatch.accept(match);
                });
    }

    public List<TeamStanding> getStandings(TournamentZone zone){
        Map<Team,TeamStanding> standings= new LinkedHashMap<>();
        zone.getTeams().forEach(team->standings.put(team,new TeamStanding(team)));
        matches.stream()
                .filter(GroupMatch.class::isInstance)
                .map(GroupMatch.class::cast)
                .filter(Match::isPlayed)
                .filter(match ->
                        standings.containsKey(match.getHomeTeam())
                                && standings.containsKey(
                                match.getAwayTeam()
                        )
                )
                .forEach(match -> {
                    TeamStanding homeStanding =
                            standings.get(match.getHomeTeam());

                    TeamStanding awayStanding =
                            standings.get(match.getAwayTeam());

                    homeStanding.registerMatch(
                            match.getHomeGoals(),
                            match.getAwayGoals()
                    );

                    awayStanding.registerMatch(
                            match.getAwayGoals(),
                            match.getHomeGoals()
                    );
                });

        return standings.values().stream()
                .sorted(
                        Comparator
                                .comparingInt(
                                        TeamStanding::getPoints
                                )
                                .reversed()
                                .thenComparing(
                                        Comparator
                                                .comparingInt(
                                                        TeamStanding
                                                                ::getGoalDifference
                                                )
                                                .reversed()
                                )
                                .thenComparing(
                                        Comparator
                                                .comparingInt(
                                                        TeamStanding
                                                                ::getGoalsFor
                                                )
                                                .reversed()
                                )
                                .thenComparingInt(
                                        standing ->
                                                standing
                                                        .getTeam()
                                                        .getRanking()
                                )
                )
                .toList();
    }

    public void loadVenues(List<City> cities, List<Stadium> stadiums) {
        if (cities == null || stadiums == null) {
            throw new IllegalArgumentException(
                    "Cities and stadiums cannot be null."
            );
        }
        for (Stadium stadium : stadiums) {
            boolean cityExists =
                    cities.stream()
                            .anyMatch(city ->
                                    city.getId()
                                            == stadium
                                            .getCity()
                                            .getId()
                            );
            if (!cityExists) {
                throw new IllegalArgumentException(
                        "Stadium "
                                + stadium.getName()
                                + " references an unknown city."
                );
            }
        }
        this.cities.clear();
        this.cities.addAll(cities);
        this.stadiums.clear();
        this.stadiums.addAll(stadiums);
    }

    public List<TournamentZone> getTournamentZones() { return Collections.unmodifiableList(tournamentZones); }
    public List<Match> getMatches() { return Collections.unmodifiableList(matches); }
    public List<Team> getTeams() { return Collections.unmodifiableList(teams); }
    public List<Referee> getReferees() { return Collections.unmodifiableList(referees); }
    public List<Country> getCountries() { return Collections.unmodifiableList(countries); }
    public List<City> getCities() {return Collections.unmodifiableList(cities);}
    public List<Stadium> getStadiums() {return Collections.unmodifiableList(stadiums);}

    @Serial
    private Object readResolve() {
        this.random = new Random();
        this.matchSimulator = new MatchSimulator(this.random);
        restoreMissingMatchdays();
        return this;
    }

    private void restoreMissingMatchdays() {
        matches.stream()
                .filter(GroupMatch.class::isInstance)
                .map(GroupMatch.class::cast)
                .filter(match ->
                        match.getMatchday() == 0
                )
                .forEach(match ->
                        match.setMatchday(
                                findMatchday(match)
                        )
                );
    }

    private int findMatchday(GroupMatch match) {
        TournamentZone zone =
                tournamentZones.stream()
                        .filter(candidate ->
                                candidate
                                        .getTeams()
                                        .contains(
                                                match.getHomeTeam()
                                        )
                                        &&
                                        candidate
                                                .getTeams()
                                                .contains(
                                                        match.getAwayTeam()
                                                )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Could not find the zone "
                                                + "for a saved match."
                                )
                        );

        int homeIndex =
                zone.getTeams().indexOf(
                        match.getHomeTeam()
                );

        int awayIndex =
                zone.getTeams().indexOf(
                        match.getAwayTeam()
                );

        if (
                isPair(homeIndex, awayIndex, 0, 3)
                        || isPair(
                        homeIndex,
                        awayIndex,
                        1,
                        2
                )
        ) {
            return 1;
        }

        if (
                isPair(homeIndex, awayIndex, 3, 2)
                        || isPair(
                        homeIndex,
                        awayIndex,
                        0,
                        1
                )
        ) {
            return 2;
        }

        return 3;
    }

    private boolean isPair(
            int first,
            int second,
            int expectedFirst,
            int expectedSecond
    ) {
        return (
                first == expectedFirst
                        && second == expectedSecond
        ) || (
                first == expectedSecond
                        && second == expectedFirst
        );
    }

    public static void main(String[] args) throws IOException {
        ChampionshipRepository repository = new ChampionshipRepository();
        Championship championship = null;

        Scanner scanner = new Scanner(System.in);
        if (repository.exists()) {
            System.out.println("==========================================");
            System.out.println("Se encontró una partida guardada previamente.");
            System.out.println("1. Continuar torneo guardado");
            System.out.println("2. Iniciar un nuevo torneo (se sobrescribirá el anterior)");
            System.out.print("Seleccione una opción: ");

            int opcion = scanner.nextInt();
            scanner.nextLine();
            if (opcion == 1) {
                championship = repository.load();
            }
        }
        if (championship == null) {
            System.out.println("\nInicializando un nuevo torneo desde torneo.json...");
            championship = new Championship("torneo.json");
        }
        MatchConsoleReporter matchReporter =
                new MatchConsoleReporter();

        StandingsConsoleReporter standingsReporter =
                new StandingsConsoleReporter();

        if (championship.hasPendingGroupMatchdays()) {

            while (championship.hasPendingGroupMatchdays()) {
                int matchday =
                        championship.getCurrentMatchday();

                System.out.println();
                System.out.println(
                        "================ MATCHDAY "
                                + matchday
                                + " ================"
                );

                championship.simulateNextMatchday(
                        matchReporter::printMatch
                );

                for (
                        TournamentZone zone
                        : championship.getTournamentZones()
                ) {
                    standingsReporter.printZone(
                            zone,
                            championship.getStandings(zone)
                    );
                }

                repository.save(championship);

                if (championship.hasPendingGroupMatchdays()) {
                    System.out.print(
                            "\nPress Enter to simulate "
                                    + "the next matchday..."
                    );

                    scanner.nextLine();
                }
            }

        } else {
            System.out.println();
            System.out.println(
                    "La fase de grupos ya estaba completa."
            );

            System.out.println(
                    "Resultados y posiciones guardadas:"
            );

            championship.getMatches()
                    .forEach(matchReporter::printMatch);

            for (
                    TournamentZone zone
                    : championship.getTournamentZones()
            ) {
                standingsReporter.printZone(
                        zone,
                        championship.getStandings(zone)
                );
            }
        }

        System.out.println();
        System.out.println(
                "Estado actual de la fase de grupos guardado."
        );

        repository.save(championship);
        scanner.close();
    }
}
